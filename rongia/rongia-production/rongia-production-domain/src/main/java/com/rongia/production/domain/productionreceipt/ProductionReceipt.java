package com.rongia.production.domain.productionreceipt;

import com.rongia.shared.domain.aggregate.OrderAggregateRoot;
import com.rongia.shared.domain.enums.SerialNumberTypeEnum;
import com.rongia.shared.domain.exception.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Nationalized;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 生产入库单
 *
 * <p>为什么不给工单加个「已入库数量」就完事：入库是「一次交付」，得留下什么时候进的、进了哪个仓（库位）、
 * 申报多少与实收多少。工单上那个累计数只是这些单据的合计，光看它对不出任何一笔，也就没法追短装。
 *
 * <p>没有行：一张工单只产出一个 SKU，入库天然是一单一货。真要用到联产品、副产品，或者一张单收好几张工单， 再拆行不迟。
 *
 * <p>生命周期与生产领料单同一套，只有两态：{@code DRAFT → POSTED}。入库那一下同时是审核：实收数量落定、计入工单
 * 已入库、货进成品仓，三件事在同一个动作里完成。草稿不动库存也不动工单，直接删掉即可，所以没有作废状态。已入库的
 * 不在这张单上翻状态——货真进过仓，冲它是在库存那边照原库存记录开一张红字单，本单停在已入库当历史记录， 「被冲过没有」去库存那边查指着它的红字单。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "ProductionReceipt",
    indexes = {
      @Index(name = "IX_ProductionReceipt_WorkOrderId", columnList = "WorkOrderId"),
      @Index(name = "IX_ProductionReceipt_MaterialId", columnList = "MaterialId"),
      @Index(name = "IX_ProductionReceipt_VariantId", columnList = "VariantId"),
      @Index(name = "IX_ProductionReceipt_Status", columnList = "Status")
    })
public class ProductionReceipt extends OrderAggregateRoot {

  /** 工单Id */
  @Column(name = "WorkOrderId", nullable = false)
  private Long workOrderId;

  /** 工单号（快照，由领域服务从工单抄） */
  @Nationalized
  @Column(name = "WorkOrderNumber", nullable = false, length = 30)
  private String workOrderNumber;

  /** 产出SPU（快照） */
  @Column(name = "MaterialId", nullable = false)
  private Long materialId;

  /** 产出SKU（快照） */
  @Column(name = "VariantId", nullable = false)
  private Long variantId;

  /** 单位Id（快照） */
  @Column(name = "UnitId", nullable = false)
  private Long unitId;

  /** 入库仓库Id */
  @Column(name = "WarehouseId", nullable = false)
  private Long warehouseId;

  /** 库位Id（可空，空表示数量只记到仓库这一层） */
  /** 申报入库数量（车间报上来的数，不填就取工单还没入库的完工数） */
  @Column(name = "AppliedQty", nullable = false, precision = 18, scale = 2)
  private BigDecimal appliedQty;

  /** 实收数量（审核时落定，0 表示尚未核定） */
  @Column(name = "ReceivedQty", nullable = false, precision = 18, scale = 2)
  private BigDecimal receivedQty;

  /** 入库时间（可空，转入已入库时写入） */
  @Column(name = "ReceiptTime")
  @Nullable
  private LocalDateTime receiptTime;

  /** 状态编码，取值见 {@link ProductionReceiptStatusEnum} */
  @Nationalized
  @Column(name = "Status", nullable = false, length = 50)
  private String status;

  /** 备注（可空） */
  @Nationalized
  @Column(name = "Remark", length = 500)
  @Nullable
  private String remark;

  /**
   * 构造器
   *
   * @param workOrderId 工单Id
   * @param workOrderNumber 工单号（快照）
   * @param materialId 产出SPU（快照）
   * @param variantId 产出SKU（快照）
   * @param unitId 单位Id（快照）
   * @param warehouseId 入库仓库Id
   * @param appliedQty 申报入库数量
   * @param remark 备注（可空）
   * @param number 单号（传空串表示由 OrderAddedEvent 的监听器后续分配）
   */
  ProductionReceipt(
      long workOrderId,
      String workOrderNumber,
      long materialId,
      long variantId,
      long unitId,
      long warehouseId,
      BigDecimal appliedQty,
      @Nullable String remark,
      String number) {
    super(number);
    apply(
        workOrderId,
        workOrderNumber,
        materialId,
        variantId,
        unitId,
        warehouseId,
        appliedQty,
        remark);
    this.status = ProductionReceiptStatusEnum.DRAFT.getCode();
  }

  /**
   * 流水号类型
   *
   * @return 生产入库单类型
   */
  @Override
  public SerialNumberTypeEnum getSerialNumberType() {
    return SerialNumberTypeEnum.PRODUCTION_RECEIPT;
  }

  /**
   * 修改单据（仅草稿可改）
   *
   * <p>改申报数量会把实收数量清零重核：核定数是照着申报数批的，申报一变，先前那个批数就不作数了。
   *
   * @param workOrderId 工单Id
   * @param workOrderNumber 工单号（快照）
   * @param materialId 产出SPU（快照）
   * @param variantId 产出SKU（快照）
   * @param unitId 单位Id（快照）
   * @param warehouseId 入库仓库Id
   * @param appliedQty 申报入库数量
   * @param remark 备注（可空）
   * @throws DomainException 已入库时抛出
   */
  public void modify(
      long workOrderId,
      String workOrderNumber,
      long materialId,
      long variantId,
      long unitId,
      long warehouseId,
      BigDecimal appliedQty,
      @Nullable String remark) {
    ensureDraft();
    apply(
        workOrderId,
        workOrderNumber,
        materialId,
        variantId,
        unitId,
        warehouseId,
        appliedQty,
        remark);
  }

  /**
   * 核定实收数量（仅草稿可用，状态不变）
   *
   * <p>仓管点货只会比车间申报的少不会多：短装就体现在这对数字上，要多收得回去改申报数。
   *
   * @param qty 实收数量，传 {@code null} 表示照批申报数量
   * @throws DomainException 已入库时抛出
   */
  public void approve(@Nullable BigDecimal qty) {
    ensureDraft();
    if (qty == null) {
      this.receivedQty = this.appliedQty;
      return;
    }
    if (qty.signum() <= 0) {
      throw new DomainException("实收数量必须大于0");
    }
    if (qty.compareTo(this.appliedQty) > 0) {
      throw new DomainException("实收数量" + qty + "不能大于申报数量" + this.appliedQty);
    }
    this.receivedQty = qty;
  }

  /**
   * 入库：实收数量落定，单据转入已入库并记下入库时间
   *
   * <p>审核与入库合成一步。实收数量没填就照批申报数；把实收数量累加到工单由 {@code ProductionReceiptDomainService}
   * 编排，货进成品仓由应用层在同一事务里调库存服务。
   *
   * @param time 入库时间
   * @throws DomainException 非草稿状态时抛出
   */
  public void post(LocalDateTime time) {
    ensureDraft();
    if (this.receivedQty.signum() == 0) {
      this.receivedQty = this.appliedQty;
    }
    this.status = ProductionReceiptStatusEnum.POSTED.getCode();
    this.receiptTime = time;
  }

  /**
   * 是否草稿
   *
   * @return true 表示草稿
   */
  public boolean isDraft() {
    return ProductionReceiptStatusEnum.DRAFT.getCode().equals(this.status);
  }

  /**
   * 是否已入库
   *
   * @return true 表示已入库
   */
  public boolean isPosted() {
    return ProductionReceiptStatusEnum.POSTED.getCode().equals(this.status);
  }

  // ==================== 私有：校验与赋值 ====================

  /**
   * 统一的校验与赋值入口（构造器与修改共用）
   *
   * @param workOrderId 工单Id
   * @param workOrderNumber 工单号（快照）
   * @param materialId 产出SPU（快照）
   * @param variantId 产出SKU（快照）
   * @param unitId 单位Id（快照）
   * @param warehouseId 入库仓库Id
   * @param appliedQty 申报入库数量
   * @param remark 备注（可空）
   * @throws DomainException 工单、快照字段、仓库非法或申报数量不为正时抛出
   */
  private void apply(
      long workOrderId,
      String workOrderNumber,
      long materialId,
      long variantId,
      long unitId,
      long warehouseId,
      BigDecimal appliedQty,
      @Nullable String remark) {
    if (workOrderId <= 0) {
      throw new DomainException("工单不能为空");
    }
    String trimmedWorkOrderNumber = StringUtils.trimToNull(workOrderNumber);
    if (trimmedWorkOrderNumber == null) {
      throw new DomainException("工单号不能为空");
    }
    if (materialId <= 0) {
      throw new DomainException("产出SPU不能为空");
    }
    if (variantId <= 0) {
      throw new DomainException("产出SKU不能为空");
    }
    if (unitId <= 0) {
      throw new DomainException("单位不能为空");
    }
    if (warehouseId <= 0) {
      throw new DomainException("入库仓库不能为空");
    }
    if (appliedQty.signum() <= 0) {
      throw new DomainException("申报入库数量必须大于0");
    }

    this.workOrderId = workOrderId;
    this.workOrderNumber = trimmedWorkOrderNumber;
    this.materialId = materialId;
    this.variantId = variantId;
    this.unitId = unitId;
    this.warehouseId = warehouseId;
    this.appliedQty = appliedQty;
    this.receivedQty = BigDecimal.ZERO;
    this.remark = remark;
  }

  /**
   * 校验单据仍处于草稿
   *
   * @throws DomainException 已入库时抛出
   */
  private void ensureDraft() {
    if (isPosted()) {
      throw new DomainException("入库单已入库，不能再修改");
    }
  }
}
