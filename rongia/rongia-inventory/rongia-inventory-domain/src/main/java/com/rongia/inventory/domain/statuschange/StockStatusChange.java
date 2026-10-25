package com.rongia.inventory.domain.statuschange;

import com.rongia.shared.domain.aggregate.OrderAggregateRoot;
import com.rongia.shared.domain.enums.SerialNumberTypeEnum;
import com.rongia.shared.domain.exception.DomainException;
import com.rongia.shared.domain.util.EnumUtils;
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
import java.util.List;
import java.util.Objects;
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
 * 库存状态转换单（把某一格上的一批数量，从一个库存状态挪到另一个状态）
 *
 * <p>放行是待检 → 可用，扣住是可用 → 冻结，查清了是冻结 → 可用，判定报废是可用 → 废品。处置虽然不同，
 * 动作是同一个：库位不动、规格不动、总量不动，只有这批数量「能不能动用」变了。所以一张单换方向就够，
 * 不为每种处置各建一张单——金蝶把这三样合在「库存状态转换」一张单里（旁边才是形态转换、批号调整两张），
 * SAP 用移动类型 321/322（质检 ↔ 非限制）、343/344（冻结 ↔ 非限制）表达同一件事，D365 走库存状态变更。
 *
 * <p>为什么必须是一张单，而不是让应用层各开一张出库单、一张入库单凑：凑出来的两笔中间断过一次，
 * 待检少了 30 而可用没加上 30，这批数量就从账上消失了，也没有一张单能审核。SAP、金蝶、D365 都把
 * 状态挪动做成一张单，正是这个原因。
 *
 * <p>来源可以空着：质量判定（来料检验单、客诉单）还没建，现在只能手工开。三个来源字段要么都填、
 * 要么都不填，半填等于说不清这批为什么挪。
 *
 * <p>过账那一行动作是「从状态那行减、到状态那行加」成对做完，不在这里拆开交给应用层——与
 * {@code StockRecord} 的规矩不同，那里加减库存要连着外部业务单（领料单核销、工单完工），编排不归库存域；
 * 这里两半是同一笔账，只走一半就是把数量弄丢。调用方仍要把整张单的过账放在同一个事务里。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "StockStatusChange",
    indexes = {
      @Index(
          name = "IX_StockStatusChange_SourceType_SourceId",
          columnList = "SourceType,SourceId"),
      @Index(name = "IX_StockStatusChange_Status", columnList = "Status")
    })
public class StockStatusChange extends OrderAggregateRoot {

  /** 来源单据类型编码，取值见 {@link SerialNumberTypeEnum}（可空，还没对应单据时手工开单就留空） */
  @Nationalized
  @Column(name = "SourceType", length = 50)
  @Nullable
  private String sourceType;

  /** 来源单据Id（可空） */
  @Column(name = "SourceId")
  @Nullable
  private Long sourceId;

  /** 来源单据号（快照，可空） */
  @Nationalized
  @Column(name = "SourceNumber", length = 30)
  @Nullable
  private String sourceNumber;

  /** 状态：草稿、已过账、已作废 */
  @Nationalized
  @Column(name = "Status", nullable = false, length = 50)
  private String status;

  /** 经办人Id（可空，做放行或判废的人） */
  @Column(name = "OperatorId")
  @Nullable
  private Long operatorId;

  /** 过账时间（可空，转入已过账时写入） */
  @Column(name = "PostedTime")
  @Nullable
  private LocalDateTime postedTime;

  /** 备注（可空，写判定结论） */
  @Nationalized
  @Column(name = "Remark", length = 500)
  @Nullable
  private String remark;

  /** 转换单行 */
  @OneToMany(
      fetch = FetchType.EAGER,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE},
      orphanRemoval = true)
  @JoinColumn(name = "StockStatusChangeId")
  @SQLRestriction("IsDeleted = 0")
  private List<StockStatusChangeLine> stockStatusChangeLines = new ArrayList<>();

  /**
   * 构造器
   *
   * @param sourceType 来源单据类型编码（可空）
   * @param sourceId 来源单据Id（可空）
   * @param sourceNumber 来源单据号（可空）
   * @param operatorId 经办人Id（可空）
   * @param remark 备注（可空）
   * @param number 单号（传空串表示由 OrderAddedEvent 的监听器后续分配）
   */
  StockStatusChange(
      @Nullable String sourceType,
      @Nullable Long sourceId,
      @Nullable String sourceNumber,
      @Nullable Long operatorId,
      @Nullable String remark,
      String number) {
    super(number);
    apply(sourceType, sourceId, sourceNumber);
    this.operatorId = operatorId;
    this.remark = remark;
    this.status = StockStatusChangeStatusEnum.DRAFT.getCode();
  }

  /**
   * 流水号类型
   *
   * @return 库存状态转换单类型
   */
  @Override
  public SerialNumberTypeEnum getSerialNumberType() {
    return SerialNumberTypeEnum.STOCK_STATUS_CHANGE;
  }

  /**
   * 获取转换单行（只读）
   *
   * @return 不可变列表
   */
  public List<StockStatusChangeLine> getStockStatusChangeLines() {
    return Collections.unmodifiableList(stockStatusChangeLines);
  }

  /**
   * 添加转换单行（仅草稿可加）
   *
   * @param materialId 物料Id（快照）
   * @param variantId 规格型号Id
   * @param warehouseId 仓库Id
   * @param fromStockStatus 转出状态编码
   * @param toStockStatus 转入状态编码
   * @param qty 挪动数量（正数）
   * @param remark 备注（可空）
   * @throws DomainException 非草稿状态，或同一格上同一对状态已在单内时抛出
   */
  public void addLine(
      long materialId,
      long variantId,
      long warehouseId,
      String fromStockStatus,
      String toStockStatus,
      BigDecimal qty,
      @Nullable String remark) {
    ensureDraft();
    if (findLine(variantId, warehouseId, fromStockStatus, toStockStatus) != null) {
      throw new DomainException("物料" + variantId + "在同一格上这条挪动已在本单中!");
    }
    stockStatusChangeLines.add(
        new StockStatusChangeLine(
            materialId,
            variantId,
            warehouseId,
            fromStockStatus,
            toStockStatus,
            qty,
            remark));
  }

  /**
   * 过账：单据转入已过账并记下过账时间
   *
   * <p>状态那两下加减由领域服务按行完成，本方法只把单据落定，避免同一张单重复挪账。
   *
   * @param time 过账时间
   * @throws DomainException 非草稿状态、或单内没有行时抛出
   */
  public void post(LocalDateTime time) {
    ensureDraft();
    if (stockStatusChangeLines.isEmpty()) {
      throw new DomainException("转换单没有行，不能过账");
    }
    this.status = StockStatusChangeStatusEnum.POSTED.getCode();
    this.postedTime = time;
  }

  /**
   * 作废：草稿不用了撕掉
   *
   * <p>已过账的作废不了：状态已经挪过，改状态改不回去，要纠正就开一张反向的转换单。
   *
   * @throws DomainException 已过账或已作废时抛出
   */
  public void cancel() {
    if (isVoided()) {
      throw new DomainException("转换单已作废，不能再作废");
    }
    if (isPosted()) {
      throw new DomainException("转换单已过账，只能开反向单纠正，不能作废");
    }
    this.status = StockStatusChangeStatusEnum.VOIDED.getCode();
  }

  /**
   * 是否草稿
   *
   * @return true 表示草稿
   */
  public boolean isDraft() {
    return StockStatusChangeStatusEnum.DRAFT.getCode().equals(this.status);
  }

  /**
   * 是否已过账
   *
   * @return true 表示已过账
   */
  public boolean isPosted() {
    return StockStatusChangeStatusEnum.POSTED.getCode().equals(this.status);
  }

  /**
   * 是否已作废
   *
   * @return true 表示已作废
   */
  public boolean isVoided() {
    return StockStatusChangeStatusEnum.VOIDED.getCode().equals(this.status);
  }

  // ==================== 私有：校验与赋值 ====================

  /**
   * 统一的校验与赋值入口
   *
   * <p>三个来源字段要么全空、要么全有，且类型必须是流水号类型枚举里的一种（检验单还没建，等它建了
   * 就有编码可填）。
   *
   * @param sourceType 来源单据类型编码（可空）
   * @param sourceId 来源单据Id（可空）
   * @param sourceNumber 来源单据号（可空）
   * @throws DomainException 来源填了一半，或来源类型不在枚举内时抛出
   */
  private void apply(
      @Nullable String sourceType, @Nullable Long sourceId, @Nullable String sourceNumber) {
    String trimmedSourceType = StringUtils.trimToNull(sourceType);
    String trimmedSourceNumber = StringUtils.trimToNull(sourceNumber);
    boolean hasAny =
        trimmedSourceType != null || sourceId != null || trimmedSourceNumber != null;
    if (!hasAny) {
      this.sourceType = null;
      this.sourceId = null;
      this.sourceNumber = null;
      return;
    }
    if (trimmedSourceType == null || sourceId == null) {
      throw new DomainException("来源要么整填、要么整空：只填一半就查不回是哪张单让这批货挪的状态");
    }
    this.sourceType = EnumUtils.getCode(SerialNumberTypeEnum.class, trimmedSourceType, "来源单据类型");
    if (sourceId <= 0) {
      throw new DomainException("来源单据Id有误!");
    }

    this.sourceId = sourceId;
    this.sourceNumber = trimmedSourceNumber;
  }

  /** 校验仍是草稿 */
  private void ensureDraft() {
    if (!isDraft()) {
      throw new DomainException("转换单不是草稿，不能改");
    }
  }

  /**
   * 按位置与状态对查找未删除的行
   *
   * <p>同一格上同一条挪动记两遍，过账时状态就会挪两次，数量当场对不上。从状态相同但到状态不同是合法的
   * （一批里 10 片放行、5 片判废），所以判重要连到状态一起看。
   *
   * @param variantId 规格型号Id
   * @param warehouseId 仓库Id
   * @param fromStockStatus 转出状态编码
   * @param toStockStatus 转入状态编码
   * @return 匹配到的行，找不到返回 {@code null}
   */
  @Nullable
  private StockStatusChangeLine findLine(
      long variantId,
      long warehouseId,
      String fromStockStatus,
      String toStockStatus) {
    return stockStatusChangeLines.stream()
        .filter(
            l ->
                Objects.equals(l.getVariantId(), variantId)
                    && Objects.equals(l.getWarehouseId(), warehouseId)
                    && Objects.equals(l.getFromStockStatus(), fromStockStatus)
                    && Objects.equals(l.getToStockStatus(), toStockStatus))
        .findFirst()
        .orElse(null);
  }
}
