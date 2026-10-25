package com.rongia.production.domain.productionreturn;

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
 * 生产退料单
 *
 * <p>与生产领料单分开成两张：领料是「料往工单走」，退料是「料往仓库回」，方向相反、各自一套单号，纸面上不会混。 金蝶云星空也是这么拆的（生产领料单 / 生产退料单）；用友与金蝶 K3
 * 用红字出库单、SAP 用 262 移动类型， 那是把退料当负向领料记，查询时还得靠正负号区分，拆开更省事。
 *
 * <p>退料不是红冲：料是领对了的，只是没用完或不合格，退回仓库要留下这一来一回。红冲说的是「这张单开错了」，
 * 冲的是整张单的额度；退料不碰原生产领料单，也不占它的红冲额度，只减工单用料的已退数量、加仓库的库存。
 *
 * <p>生命周期与生产领料单同一套，只有两态：{@code DRAFT → POSTED}。退料那一下同时是审核：核定数量计入工单用料的
 * 已退数量、货回到仓库，三件事在同一个动作里完成。草稿不动库存也不动工单用料，直接删掉即可，所以没有作废状态。
 * 已退料的不在这张单上翻状态——货真回过仓库，冲它是在库存那边照原库存记录开一张红字单，本单停在已退料当历史 记录。
 *
 * <p>库存那一半（回仓入库）不在这里做：本模块不依赖库存模块，应用层在退料这一步调 {@code StockDomainService} 按行上的仓库与库位把货加回去。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "ProductionReturn",
    indexes = {
      @Index(name = "IX_ProductionReturn_WorkOrderId", columnList = "WorkOrderId"),
      @Index(name = "IX_ProductionReturn_Status", columnList = "Status")
    })
public class ProductionReturn extends OrderAggregateRoot {

  /** 工单Id */
  @Column(name = "WorkOrderId", nullable = false)
  private Long workOrderId;

  /** 工单号（冗余，便于展示） */
  @Nationalized
  @Column(name = "WorkOrderNumber", nullable = false, length = 30)
  private String workOrderNumber;

  /** 经办人Id（可空，退料人） */
  @Column(name = "OperatorId")
  @Nullable
  private Long operatorId;

  /** 退料时间（可空，转入已退料时写入） */
  @Column(name = "ReturnTime")
  @Nullable
  private LocalDateTime returnTime;

  /** 状态编码，取值见 {@link ProductionReturnStatusEnum} */
  @Nationalized
  @Column(name = "Status", nullable = false, length = 50)
  private String status;

  /** 备注（可空） */
  @Nationalized
  @Column(name = "Remark", length = 500)
  @Nullable
  private String remark;

  /** 生产退料单行 */
  @OneToMany(
      fetch = FetchType.EAGER,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE},
      orphanRemoval = true)
  @JoinColumn(name = "ProductionReturnId")
  @SQLRestriction("IsDeleted = 0")
  private List<ProductionReturnLine> productionReturnLines = new ArrayList<>();

  /**
   * 构造器
   *
   * @param workOrderId 工单Id
   * @param workOrderNumber 工单号（冗余，便于展示）
   * @param operatorId 经办人Id（可空）
   * @param remark 备注（可空）
   * @param number 单号（传空串表示由 OrderAddedEvent 的监听器后续分配）
   */
  ProductionReturn(
      long workOrderId,
      String workOrderNumber,
      @Nullable Long operatorId,
      @Nullable String remark,
      String number) {
    super(number);
    apply(workOrderId, workOrderNumber, operatorId, remark);
    this.status = ProductionReturnStatusEnum.DRAFT.getCode();
  }

  /**
   * 流水号类型
   *
   * @return 生产退料单类型
   */
  @Override
  public SerialNumberTypeEnum getSerialNumberType() {
    return SerialNumberTypeEnum.PRODUCTION_RETURN;
  }

  /**
   * 获取生产退料单行（只读）
   *
   * @return 不可变列表
   */
  public List<ProductionReturnLine> getProductionReturnLines() {
    return Collections.unmodifiableList(productionReturnLines);
  }

  /**
   * 修改单据头（仅草稿可改）
   *
   * @param workOrderId 工单Id
   * @param workOrderNumber 工单号（冗余，便于展示）
   * @param operatorId 经办人Id（可空）
   * @param remark 备注（可空）
   * @throws DomainException 已退料时抛出
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
   * 添加退料行（仅草稿可加）
   *
   * @param workOrderMaterialId 工单用料行Id
   * @param materialId 物料SPU（快照）
   * @param variantId 物料SKU（快照）
   * @param appliedQty 申请退料数量
   * @param unitId 单位Id（快照）
   * @param warehouseId 退料仓库Id
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
    productionReturnLines.add(
        new ProductionReturnLine(
            workOrderMaterialId,
            materialId,
            variantId,
            appliedQty,
            unitId,
            warehouseId,
            remark));
  }

  /**
   * 删除退料行（仅草稿可删，逻辑删除）
   *
   * @param workOrderMaterialId 工单用料行Id
   * @throws DomainException 已退料时抛出
   */
  public void deleteLine(long workOrderMaterialId) {
    ensureDraft();
    ProductionReturnLine line = findLine(workOrderMaterialId);
    if (line == null) {
      return;
    }
    line.delete();
  }

  /**
   * 核定：把仓管点收下来的数量逐行记上，状态仍是草稿
   *
   * <p>没在审批清单里点到的行留待 {@link #post(LocalDateTime)} 按申请数照批。一条用料行只能给一个核定数， 重复提交说明调用方拼错了清单。
   *
   * @param approvals 逐行核定清单
   * @throws DomainException 非草稿状态、用料行不在单内、或核定数量非法时抛出
   */
  public void approve(List<ProductionReturnApprovalData> approvals) {
    ensureDraft();
    Set<Long> seen = new HashSet<>();
    for (ProductionReturnApprovalData data : approvals) {
      if (!seen.add(data.workOrderMaterialId())) {
        throw new DomainException("工单用料" + data.workOrderMaterialId() + "重复核定!");
      }
      ProductionReturnLine line = findLine(data.workOrderMaterialId());
      if (line == null) {
        throw new DomainException("工单用料" + data.workOrderMaterialId() + "不在本单中!");
      }
      line.approve(data.approvedQty());
    }
  }

  /**
   * 退料：未点定的行按申请数照批，单据转入已退料
   *
   * <p>审核与退料合成一步：核定数量落到工单用料那边由 {@code ProductionReturnDomainService} 编排，货回到仓库由
   * 应用层在同一事务里调库存服务，本方法只管自己这一半。
   *
   * @param time 退料时间
   * @throws DomainException 非草稿状态、或单内没有行时抛出
   */
  public void post(LocalDateTime time) {
    ensureDraft();
    if (productionReturnLines.isEmpty()) {
      throw new DomainException("退料单没有行，不能退料");
    }
    for (ProductionReturnLine line : productionReturnLines) {
      if (line.isNotApproved()) {
        line.approve(line.getAppliedQty());
      }
    }
    this.status = ProductionReturnStatusEnum.POSTED.getCode();
    this.returnTime = time;
  }

  /**
   * 是否草稿
   *
   * @return true 表示草稿
   */
  public boolean isDraft() {
    return ProductionReturnStatusEnum.DRAFT.getCode().equals(this.status);
  }

  /**
   * 是否已退料
   *
   * @return true 表示已退料
   */
  public boolean isPosted() {
    return ProductionReturnStatusEnum.POSTED.getCode().equals(this.status);
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
   * @throws DomainException 已退料时抛出
   */
  private void ensureDraft() {
    if (isPosted()) {
      throw new DomainException("退料单已退料，不能再修改");
    }
  }

  /**
   * 按工单用料行Id查找未删除的退料行
   *
   * @param workOrderMaterialId 工单用料行Id
   * @return 匹配到的退料行，找不到返回 {@code null}
   */
  @Nullable
  private ProductionReturnLine findLine(long workOrderMaterialId) {
    return productionReturnLines.stream()
        .filter(
            l ->
                Objects.equals(l.getWorkOrderMaterialId(), workOrderMaterialId)
                    && !l.getIsDeleted())
        .findFirst()
        .orElse(null);
  }
}
