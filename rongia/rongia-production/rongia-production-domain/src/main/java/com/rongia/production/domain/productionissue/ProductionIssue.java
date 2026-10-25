package com.rongia.production.domain.productionissue;

import com.rongia.shared.domain.aggregate.OrderAggregateRoot;
import com.rongia.shared.domain.enums.SerialNumberTypeEnum;
import com.rongia.shared.domain.exception.DomainException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.SQLRestriction;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 生产领料单
 *
 * <p>为什么独立成聚合而不是给工单用料行加几个字段：领料是「一次交付」，天然带申请人、核定人、发料仓库、发生时间。
 * 拍平成一个累计值之后，谁领的、超了没有、谁批的、从哪个仓发的全部查不回来，超领也就没法审批。
 *
 * <p>生命周期只有两态：{@code DRAFT → POSTED}。发料那一下同时是审核：核定数量落到行上、计入工单用料的已领
 * 数量、货从仓库出去，三件事在同一个动作里完成，中间不留「批了还没发」的窗口。草稿不动库存也不动工单用料，
 * 直接删掉即可，所以没有作废状态。已发料的单子不在这张单上翻状态——货真出过仓库，冲它是在库存那边照原库存 单据开一张红字单，本单停在已发料当历史记录，「被冲过没有」去库存那边查指着它的红字单。
 *
 * <p>退料是另一张单（{@code ProductionReturn}）：生产领料单只管料往工单出，一张单一个方向。库存那一半（出库动账）
 * 不在这里做：本模块不依赖库存模块，两个聚合之间的动账由应用层编排，生产领料单只保证单据自身与工单用料对得上。
 * 名也不用另存一份——本单的行就是占用（批准量还没发出去的那部分），库存那边的预留量只是它的合计。
 * 已发料的单子开错了，应用层照它开一张红字单把货倒回来——料一旦真出过仓，单据状态改不回去库存；草稿还没动货， 删掉重开即可。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "ProductionIssue",
    indexes = {
      @Index(name = "IX_ProductionIssue_WorkOrderId", columnList = "WorkOrderId"),
      @Index(name = "IX_ProductionIssue_Status", columnList = "Status")
    })
public class ProductionIssue extends OrderAggregateRoot {

  /** 工单Id */
  @Column(name = "WorkOrderId", nullable = false)
  private Long workOrderId;

  /** 工单号（冗余，便于展示） */
  @Nationalized
  @Column(name = "WorkOrderNumber", nullable = false, length = 30)
  private String workOrderNumber;

  /** 经办人Id（可空，领料人） */
  @Column(name = "OperatorId")
  @Nullable
  private Long operatorId;

  /** 发料时间（可空，退料单上即回库时间） */
  @Column(name = "IssueTime")
  @Nullable
  private LocalDateTime issueTime;

  /** 状态编码，取值见 {@link ProductionIssueStatusEnum} */
  @Nationalized
  @Column(name = "Status", nullable = false, length = 50)
  private String status;

  /** 备注（可空） */
  @Nationalized
  @Column(name = "Remark", length = 500)
  @Nullable
  private String remark;

  /** 生产领料单行 */
  @OneToMany(
      fetch = FetchType.EAGER,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE},
      orphanRemoval = true)
  @JoinColumn(name = "ProductionIssueId")
  @SQLRestriction("IsDeleted = 0")
  private List<ProductionIssueLine> productionIssueLines = new ArrayList<>();

  /**
   * 构造器
   *
   * @param workOrderId 工单Id
   * @param workOrderNumber 工单号（冗余，便于展示）
   * @param operatorId 经办人Id（可空）
   * @param remark 备注（可空）
   * @param number 单号（传空串表示由 OrderAddedEvent 的监听器后续分配）
   */
  ProductionIssue(
      long workOrderId,
      String workOrderNumber,
      @Nullable Long operatorId,
      @Nullable String remark,
      String number) {
    super(number);
    apply(workOrderId, workOrderNumber, operatorId, remark);
    this.status = ProductionIssueStatusEnum.DRAFT.getCode();
  }

  /**
   * 流水号类型
   *
   * @return 生产领料单类型
   */
  @Override
  public SerialNumberTypeEnum getSerialNumberType() {
    return SerialNumberTypeEnum.PRODUCTION_ISSUE;
  }

  /**
   * 获取生产领料单行（只读）
   *
   * @return 不可变列表
   */
  public List<ProductionIssueLine> getProductionIssueLines() {
    return Collections.unmodifiableList(productionIssueLines);
  }

  /**
   * 修改单据头（仅草稿可改）
   *
   * @param workOrderId 工单Id
   * @param workOrderNumber 工单号（冗余，便于展示）
   * @param operatorId 经办人Id（可空）
   * @param remark 备注（可空）
   * @throws DomainException 已发料时抛出
   */
  public void modify(
      long workOrderId,
      String workOrderNumber,
      @Nullable Long operatorId,
      @Nullable String remark) {
    ensureDraft();
    apply(workOrderId, workOrderNumber, operatorId, remark);
  }

  /**
   * 添加领料行（仅草稿可加）
   *
   * @param workOrderMaterialId 工单用料行Id
   * @param materialId 物料SPU（快照）
   * @param variantId 物料SKU（快照）
   * @param appliedQty 申请数量
   * @param unitId 单位Id（快照）
   * @param warehouseId 发料仓库Id
   * @param remark 备注（可空）
   * @throws DomainException 非草稿状态、或同一工单用料行已在单内时抛出
   */
  public void addLine(
      long workOrderMaterialId,
      long materialId,
      long variantId,
      BigDecimal appliedQty,
      long unitId,
      long warehouseId,
      @Nullable String remark) {
    ensureDraft();
    if (findLine(workOrderMaterialId) != null) {
      throw new DomainException("工单用料" + workOrderMaterialId + "已在本单中!");
    }
    productionIssueLines.add(
        new ProductionIssueLine(
            workOrderMaterialId,
            materialId,
            variantId,
            appliedQty,
            unitId,
            warehouseId,
            remark));
  }

  /**
   * 删除领料行（仅草稿可删，逻辑删除）
   *
   * @param workOrderMaterialId 工单用料行Id
   * @throws DomainException 已发料时抛出
   */
  public void deleteLine(long workOrderMaterialId) {
    ensureDraft();
    ProductionIssueLine line = findLine(workOrderMaterialId);
    if (line == null) {
      return;
    }
    line.delete();
  }

  /**
   * 核定：把仓管批下来的数量逐行记上，状态仍是草稿
   *
   * <p>没在审批清单里点到的行留待 {@link #post(LocalDateTime)} 按申请数照批。一条用料行只能给一个核定数， 重复提交说明调用方拼错了清单。
   *
   * @param approvals 逐行核定清单
   * @throws DomainException 非草稿状态、用料行不在单内、或核定数量非法时抛出
   */
  public void approve(List<ProductionIssueApprovalData> approvals) {
    ensureDraft();
    Set<Long> seen = new HashSet<>();
    for (ProductionIssueApprovalData data : approvals) {
      if (!seen.add(data.workOrderMaterialId())) {
        throw new DomainException("工单用料" + data.workOrderMaterialId() + "重复核定!");
      }
      ProductionIssueLine line = findLine(data.workOrderMaterialId());
      if (line == null) {
        throw new DomainException("工单用料" + data.workOrderMaterialId() + "不在本单中!");
      }
      line.approve(data.approvedQty());
    }
  }

  /**
   * 发料：未点定的行按申请数照批，单据转入已发料
   *
   * <p>审核与发料合成一步：核定数量落到工单用料那边由 {@code ProductionIssueDomainService} 编排，货从仓库出去
   * 由应用层在同一事务里调库存服务，本方法只管自己这一半。
   *
   * @param time 发料时间
   * @throws DomainException 非草稿状态、或单内没有行时抛出
   */
  public void post(LocalDateTime time) {
    ensureDraft();
    if (productionIssueLines.isEmpty()) {
      throw new DomainException("生产领料单没有行，不能发料");
    }
    for (ProductionIssueLine line : productionIssueLines) {
      if (line.isNotApproved()) {
        line.approve(line.getAppliedQty());
      }
    }
    this.status = ProductionIssueStatusEnum.POSTED.getCode();
    this.issueTime = time;
  }

  /**
   * 是否草稿
   *
   * @return true 表示草稿
   */
  public boolean isDraft() {
    return ProductionIssueStatusEnum.DRAFT.getCode().equals(this.status);
  }

  /**
   * 是否已发料
   *
   * @return true 表示已发料
   */
  public boolean isPosted() {
    return ProductionIssueStatusEnum.POSTED.getCode().equals(this.status);
  }

  // ==================== 私有：校验与赋值 ====================

  /**
   * 统一的校验与赋值入口（构造器与修改共用）
   *
   * @param workOrderId 工单Id
   * @param workOrderNumber 工单号（冗余，便于展示）
   * @param operatorId 经办人Id（可空）
   * @param remark 备注（可空）
   * @throws DomainException 工单或工单号非法时抛出
   */
  private void apply(
      long workOrderId,
      String workOrderNumber,
      @Nullable Long operatorId,
      @Nullable String remark) {
    if (workOrderId <= 0) {
      throw new DomainException("工单不能为空");
    }
    String trimmedWorkOrderNumber = StringUtils.trimToNull(workOrderNumber);
    if (trimmedWorkOrderNumber == null) {
      throw new DomainException("工单号不能为空");
    }
    this.workOrderId = workOrderId;
    this.workOrderNumber = trimmedWorkOrderNumber;
    this.operatorId = operatorId;
    this.remark = remark;
  }

  /**
   * 校验单据仍处于草稿
   *
   * @throws DomainException 已发料时抛出
   */
  private void ensureDraft() {
    if (isPosted()) {
      throw new DomainException("生产领料单已发料，不能再修改");
    }
  }

  /**
   * 按工单用料行Id查找未删除的领料行
   *
   * @param workOrderMaterialId 工单用料行Id
   * @return 匹配到的领料行，找不到返回 {@code null}
   */
  @Nullable
  private ProductionIssueLine findLine(long workOrderMaterialId) {
    return productionIssueLines.stream()
        .filter(
            l ->
                Objects.equals(l.getWorkOrderMaterialId(), workOrderMaterialId)
                    && !l.getIsDeleted())
        .findFirst()
        .orElse(null);
  }
}
