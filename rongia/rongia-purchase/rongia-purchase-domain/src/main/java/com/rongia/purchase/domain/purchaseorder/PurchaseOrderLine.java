package com.rongia.purchase.domain.purchaseorder;

import com.rongia.shared.domain.entity.BaseDeleteEntity;
import com.rongia.shared.domain.exception.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Nationalized;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 采购单行
 *
 * <p>一行说的是「向这个供应商买哪个 SKU、买多少、什么价、打几折、按什么税率」。物料只存 Id，供应商料号存一份快照：
 * 对照表在供应商物料主数据里，但那边会改，改完历史订单不能跟着变，对账时还得是当初那个号。
 *
 * <p>金额三列算完就落库，不是每次现算：税按行取整，一行先算净额再算税，单头只是把行加起来，现算会得到另一个数。
 *
 * <p>下单时的默认价取供应商物料卡上的采购价，供应商料号从那张卡带出来，这两件都在应用层拼，行本身只认传进来的数。
 *
 * <p>已收数量是净额：收货单一次次累加、退货单再减回来，收完没有看它跟订单量比，不看单头状态（照 Odoo 的
 * {@code qty_received}）。退货数量单独记一份，把净额加回去就是一共收到过多少，对账时要看的是这个。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "PurchaseOrderLine",
    indexes = {
      @Index(name = "IX_PurchaseOrderLine_PurchaseOrderId", columnList = "PurchaseOrderId"),
      @Index(name = "IX_PurchaseOrderLine_MaterialId", columnList = "MaterialId"),
      @Index(name = "IX_PurchaseOrderLine_VariantId", columnList = "VariantId")
    })
public class PurchaseOrderLine extends BaseDeleteEntity {

  /** 采购单Id */
  @Column(name = "PurchaseOrderId")
  private Long purchaseOrderId;

  /** 物料SPU */
  @Column(name = "MaterialId", nullable = false)
  private Long materialId;

  /** 物料SKU */
  @Column(name = "VariantId", nullable = false)
  private Long variantId;

  /** 供应商料号（可空，下单时从供应商物料卡抄成快照） */
  @Nationalized
  @Column(name = "SupplierMaterialCode", length = 100)
  @Nullable
  private String supplierMaterialCode;

  /** 数量 */
  @Column(name = "Qty", nullable = false, precision = 18, scale = 2)
  private BigDecimal qty;

  /** 已收数量（净额：收货累加、退货减回，初值 0） */
  @Column(name = "ReceivedQty", nullable = false, precision = 18, scale = 2)
  private BigDecimal receivedQty;

  /** 退货数量（退货一次次累加，初值 0） */
  @Column(name = "ReturnedQty", nullable = false, precision = 18, scale = 2)
  private BigDecimal returnedQty;

  /** 单位Id */
  @Column(name = "UnitId", nullable = false)
  private Long unitId;

  /** 单价（不含税） */
  @Column(name = "Price", nullable = false, precision = 18, scale = 2)
  private BigDecimal price;

  /** 折扣（%，0 表示不打折） */
  @Column(name = "Discount", nullable = false, precision = 18, scale = 2)
  private BigDecimal discount;

  /** 税率Id（可空，空表示本行不计税） */
  @Column(name = "TaxRateId")
  @Nullable
  private Long taxRateId;

  /** 税率快照（百分比，可空，与税率Id同进同出） */
  @Column(name = "TaxRate", precision = 18, scale = 2)
  @Nullable
  private BigDecimal taxRate;

  /** 未税金额 */
  @Column(name = "AmountUntaxed", nullable = false, precision = 18, scale = 2)
  private BigDecimal amountUntaxed;

  /** 税额 */
  @Column(name = "AmountTax", nullable = false, precision = 18, scale = 2)
  private BigDecimal amountTax;

  /** 含税金额 */
  @Column(name = "AmountTotal", nullable = false, precision = 18, scale = 2)
  private BigDecimal amountTotal;

  /**
   * 构造器
   *
   * @param materialId 物料SPU
   * @param variantId 物料SKU
   * @param supplierMaterialCode 供应商料号（可空）
   * @param qty 数量
   * @param unitId 单位Id
   * @param price 单价（不含税）
   * @param discount 折扣（%）
   * @param taxRateId 税率Id（可空）
   * @param taxRate 税率快照（百分比，可空）
   */
  PurchaseOrderLine(
      long materialId,
      long variantId,
      @Nullable String supplierMaterialCode,
      BigDecimal qty,
      long unitId,
      BigDecimal price,
      BigDecimal discount,
      @Nullable Long taxRateId,
      @Nullable BigDecimal taxRate) {
    apply(
        materialId,
        variantId,
        supplierMaterialCode,
        qty,
        unitId,
        price,
        discount,
        taxRateId,
        taxRate);
    this.receivedQty = BigDecimal.ZERO;
    this.returnedQty = BigDecimal.ZERO;
  }

  /**
   * 修改采购单行
   *
   * @param materialId 物料SPU
   * @param variantId 物料SKU
   * @param supplierMaterialCode 供应商料号（可空）
   * @param qty 数量
   * @param unitId 单位Id
   * @param price 单价（不含税）
   * @param discount 折扣（%）
   * @param taxRateId 税率Id（可空）
   * @param taxRate 税率快照（百分比，可空）
   */
  void modify(
      long materialId,
      long variantId,
      @Nullable String supplierMaterialCode,
      BigDecimal qty,
      long unitId,
      BigDecimal price,
      BigDecimal discount,
      @Nullable Long taxRateId,
      @Nullable BigDecimal taxRate) {
    apply(
        materialId,
        variantId,
        supplierMaterialCode,
        qty,
        unitId,
        price,
        discount,
        taxRateId,
        taxRate);
  }

  // ==================== 私有：校验与赋值 ====================

  /**
   * 统一的校验与赋值入口（构造器与修改共用）
   *
   * <p>金额在这里一次算定：净额 = 数量 × 单价 × (1 - 折扣)，税 = 净额 × 税率，含税 = 净额 + 税，都保留两位。
   *
   * @param materialId 物料SPU
   * @param variantId 物料SKU
   * @param supplierMaterialCode 供应商料号（可空）
   * @param qty 数量
   * @param unitId 单位Id
   * @param price 单价（不含税）
   * @param discount 折扣（%）
   * @param taxRateId 税率Id（可空）
   * @param taxRate 税率快照（百分比，可空）
   * @throws DomainException 物料、数量、单位、单价、折扣或税率非法时抛出
   */
  private void apply(
      long materialId,
      long variantId,
      @Nullable String supplierMaterialCode,
      BigDecimal qty,
      long unitId,
      BigDecimal price,
      BigDecimal discount,
      @Nullable Long taxRateId,
      @Nullable BigDecimal taxRate) {
    if (materialId <= 0) {
      throw new DomainException("物料SPU不能为空");
    }
    if (variantId <= 0) {
      throw new DomainException("物料不能为空");
    }
    if (qty.signum() <= 0) {
      throw new DomainException("数量必须大于0");
    }
    if (unitId <= 0) {
      throw new DomainException("单位不能为空");
    }
    if (price.signum() < 0) {
      throw new DomainException("单价不能为负数");
    }
    if (discount.signum() < 0 || discount.compareTo(BigDecimal.valueOf(100)) > 0) {
      throw new DomainException("折扣必须在0到100之间");
    }
    if (taxRateId != null && taxRate == null) {
      throw new DomainException("选了税率就得把税率值带上");
    }
    if (taxRateId == null && taxRate != null) {
      throw new DomainException("没有税率Id就不能填税率值");
    }

    this.materialId = materialId;
    this.variantId = variantId;
    this.supplierMaterialCode = StringUtils.trimToNull(supplierMaterialCode);
    this.qty = qty;
    this.unitId = unitId;
    this.price = price;
    this.discount = discount;
    this.taxRateId = taxRateId;
    this.taxRate = taxRate;

    BigDecimal untaxed =
        qty.multiply(price)
            .multiply(BigDecimal.ONE.subtract(divideHundred(discount)))
            .setScale(2, RoundingMode.HALF_UP);
    BigDecimal tax =
        taxRate == null
            ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            : untaxed.multiply(divideHundred(taxRate)).setScale(2, RoundingMode.HALF_UP);
    this.amountUntaxed = untaxed;
    this.amountTax = tax;
    this.amountTotal = untaxed.add(tax);
  }

  /**
   * 收货：累加已收数量
   *
   * <p>上限是订单量：收多了对账时说不清多出来的那批算哪一笔。要按超收处理，先回去把订单量调大，
   * 而不是在这里放宽校验。
   *
   * @param qty 本次收货数量
   * @throws DomainException 数量非正、或累计收货超过订单量时抛出
   */
  public void receive(BigDecimal qty) {
    if (qty.signum() <= 0) {
      throw new DomainException("收货数量必须大于0");
    }
    BigDecimal total = this.receivedQty.add(qty);
    if (total.compareTo(this.qty) > 0) {
      throw new DomainException(
          "物料" + this.variantId + "超收：订单" + this.qty + "，累计收货将达" + total);
    }
    this.receivedQty = total;
  }

  /**
   * 退货：已收数量减回去，退货数量累加上去
   *
   * <p>已收数量是净额（收多少减退多少），所以退货的上限就是当前的已收数量：退超过它，等于要把还没到的货也退掉。
   *
   * @param qty 本次退货数量
   * @throws DomainException 数量非正、或退货超过已收数量时抛出
   */
  public void unreceive(BigDecimal qty) {
    if (qty.signum() <= 0) {
      throw new DomainException("退货数量必须大于0");
    }
    if (qty.compareTo(this.receivedQty) > 0) {
      throw new DomainException(
          "物料" + this.variantId + "退货超过已收数量" + this.receivedQty);
    }
    this.receivedQty = this.receivedQty.subtract(qty);
    this.returnedQty = this.returnedQty.add(qty);
  }

  /**
   * 百分比转小数：5 表示 5%，换算成 0.05
   *
   * @param percent 百分比
   * @return 小数
   */
  private static BigDecimal divideHundred(BigDecimal percent) {
    return percent.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
  }
}
