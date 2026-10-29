package com.rongia.sales.domain.salesorder;

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
 * 销售订单行
 *
 * <p>一行说的是「客户要哪个 SKU、要多少、什么价、打几折、按什么税率」。物料只存 Id，客户料号与客户叫法存一份快照：
 * 对照表在客户物料主数据里，但那边会改，改完历史订单不能跟着变，打印给客户看的还得是当初那个号。
 *
 * <p>已发数量是净额：发货单一次次累加、退货单再减回来，发完没有看它跟订单量比，不看单头状态（照 Odoo 的
 * {@code qty_delivered}）。退货数量单独记一份，把净额加回去就是一共发出过多少，对账时要看的是这个。
 *
 * <p>金额三列算完就落库，不是每次现算：税按行取整，一行先算净额再算税，单头只是把行加起来，现算会得到另一个数。
 *
 * <p>下单时的默认价取物料上的销售价，客户料号从客户物料对照表带出来，这两件都在应用层拼，行本身只认传进来的数。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "SalesOrderLine",
    indexes = {
      @Index(name = "IX_SalesOrderLine_SalesOrderId", columnList = "SalesOrderId"),
      @Index(name = "IX_SalesOrderLine_MaterialId", columnList = "MaterialId"),
      @Index(name = "IX_SalesOrderLine_VariantId", columnList = "VariantId")
      // 注意：SalesOrderId + Sequence 的过滤唯一索引带 WHERE IsDeleted=0，JPA 不支持，用 Flyway 创建
    })
public class SalesOrderLine extends BaseDeleteEntity {

  /** 销售订单Id */
  @Column(name = "SalesOrderId")
  private Long salesOrderId;

  /** 行号 */
  @Column(name = "Sequence", nullable = false)
  private Integer sequence;

  /** 物料SPU */
  @Column(name = "MaterialId", nullable = false)
  private Long materialId;

  /** 物料SKU */
  @Column(name = "VariantId", nullable = false)
  private Long variantId;

  /** 客户料号（可空，下单时从客户物料对照表抄成快照） */
  @Nationalized
  @Column(name = "CustomerMaterialCode", length = 100)
  @Nullable
  private String customerMaterialCode;

  /** 客户叫法（可空，下单时从客户物料对照表抄成快照） */
  @Nationalized
  @Column(name = "CustomerMaterialName", length = 200)
  @Nullable
  private String customerMaterialName;

  /** 数量 */
  @Column(name = "Qty", nullable = false, precision = 18, scale = 2)
  private BigDecimal qty;

  /** 已发数量（净额：发货累加、退货减回，初值 0） */
  @Column(name = "DeliveredQty", nullable = false, precision = 18, scale = 2)
  private BigDecimal deliveredQty;

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
   * @param sequence 行号
   * @param materialId 物料SPU
   * @param variantId 物料SKU
   * @param customerMaterialCode 客户料号（可空）
   * @param customerMaterialName 客户叫法（可空）
   * @param qty 数量
   * @param unitId 单位Id
   * @param price 单价（不含税）
   * @param discount 折扣（%）
   * @param taxRateId 税率Id（可空）
   * @param taxRate 税率快照（百分比，可空）
   */
  SalesOrderLine(
      int sequence,
      long materialId,
      long variantId,
      @Nullable String customerMaterialCode,
      @Nullable String customerMaterialName,
      BigDecimal qty,
      long unitId,
      BigDecimal price,
      BigDecimal discount,
      @Nullable Long taxRateId,
      @Nullable BigDecimal taxRate) {
    apply(
        sequence,
        materialId,
        variantId,
        customerMaterialCode,
        customerMaterialName,
        qty,
        unitId,
        price,
        discount,
        taxRateId,
        taxRate);
    this.deliveredQty = BigDecimal.ZERO;
    this.returnedQty = BigDecimal.ZERO;
  }

  /**
   * 修改销售订单行
   *
   * @param sequence 行号
   * @param materialId 物料SPU
   * @param variantId 物料SKU
   * @param customerMaterialCode 客户料号（可空）
   * @param customerMaterialName 客户叫法（可空）
   * @param qty 数量
   * @param unitId 单位Id
   * @param price 单价（不含税）
   * @param discount 折扣（%）
   * @param taxRateId 税率Id（可空）
   * @param taxRate 税率快照（百分比，可空）
   */
  void modify(
      int sequence,
      long materialId,
      long variantId,
      @Nullable String customerMaterialCode,
      @Nullable String customerMaterialName,
      BigDecimal qty,
      long unitId,
      BigDecimal price,
      BigDecimal discount,
      @Nullable Long taxRateId,
      @Nullable BigDecimal taxRate) {
    apply(
        sequence,
        materialId,
        variantId,
        customerMaterialCode,
        customerMaterialName,
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
   * @param sequence 行号
   * @param materialId 物料SPU
   * @param variantId 物料SKU
   * @param customerMaterialCode 客户料号（可空）
   * @param customerMaterialName 客户叫法（可空）
   * @param qty 数量
   * @param unitId 单位Id
   * @param price 单价（不含税）
   * @param discount 折扣（%）
   * @param taxRateId 税率Id（可空）
   * @param taxRate 税率快照（百分比，可空）
   * @throws DomainException 行号、物料、数量、单位、单价、折扣或税率非法时抛出
   */
  private void apply(
      int sequence,
      long materialId,
      long variantId,
      @Nullable String customerMaterialCode,
      @Nullable String customerMaterialName,
      BigDecimal qty,
      long unitId,
      BigDecimal price,
      BigDecimal discount,
      @Nullable Long taxRateId,
      @Nullable BigDecimal taxRate) {
    if (sequence <= 0) {
      throw new DomainException("销售订单行号必须大于0");
    }
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

    this.sequence = sequence;
    this.materialId = materialId;
    this.variantId = variantId;
    this.customerMaterialCode = StringUtils.trimToNull(customerMaterialCode);
    this.customerMaterialName = StringUtils.trimToNull(customerMaterialName);
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
   * 发货：累加已发数量
   *
   * <p>上限是订单量：发多了对账时说不清多出来的那批算哪一笔。要按超发处理，先回去把订单量调大，
   * 而不是在这里放宽校验。
   *
   * @param qty 本次发货数量
   * @throws DomainException 数量非正、或累计发货超过订单量时抛出
   */
  public void deliver(BigDecimal qty) {
    if (qty.signum() <= 0) {
      throw new DomainException("发货数量必须大于0");
    }
    BigDecimal total = this.deliveredQty.add(qty);
    if (total.compareTo(this.qty) > 0) {
      throw new DomainException(
          "物料" + this.variantId + "超发：订单" + this.qty + "，累计发货将达" + total);
    }
    this.deliveredQty = total;
  }

  /**
   * 退货：已发数量减回去，退货数量累加上去
   *
   * <p>已发数量是净额（发多少减退多少），所以退货的上限就是当前的已发数量：退超过它，等于要把还没发的货也退掉。
   *
   * @param qty 本次退货数量
   * @throws DomainException 数量非正、或退货超过已发数量时抛出
   */
  public void undeliver(BigDecimal qty) {
    if (qty.signum() <= 0) {
      throw new DomainException("退货数量必须大于0");
    }
    if (qty.compareTo(this.deliveredQty) > 0) {
      throw new DomainException(
          "物料" + this.variantId + "退货超过已发数量" + this.deliveredQty);
    }
    this.deliveredQty = this.deliveredQty.subtract(qty);
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
