package com.rongia.purchase.domain.purchaseorder;

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
 * 采购单
 *
 * <p>向供应商买料的那张纸：跟谁买、什么时候下单、什么时候交货、买哪几个 SKU、各多少、什么价。一个供应商一张单，
 * 一张单一个交期；货可以分多次到，每次收了多少记到行上的已收数量，不用拆单。
 *
 * <p>金额三列（未税、税额、含税）跟着行算完就落库：税是按行取整的，单头只是把行加起来，每次现算未必算出同一个数，
 * 打印给供应商看的也得是当初那一版。
 *
 * <p>生命周期四态：{@code DRAFT} → {@code CONFIRMED}，确认后就冻住；收过的照算、剩下的不要了走 {@code CLOSED}（短交），
 * 不买了走 {@code CANCELLED}。照 Odoo 的 {@code purchase.order}：状态只说单据走到哪一步，「收完没有」不看状态，
 * 看行上的已收数量与订单量比——CLOSED 只为结案，不代表收完。
 *
 * <p>单上不挂收货单、不挂入库单、不挂应付：收了没有、入了哪个库位是库存模块的事，采购模块只认这张单要买什么。
 * 收货单靠来源单据找回这张单，口径跟销售单派工单一样。
 *
 * <p>命名对着 Odoo 的 {@code purchase.order} 与 ERPNext 的 {@code Purchase Order}。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "PurchaseOrder",
    indexes = {
      @Index(name = "IX_PurchaseOrder_SupplierId", columnList = "SupplierId"),
      @Index(name = "IX_PurchaseOrder_OrderTime", columnList = "OrderTime"),
      @Index(name = "IX_PurchaseOrder_Status", columnList = "Status")
    })
public class PurchaseOrder extends OrderAggregateRoot {

  /** 供应商Id */
  @Column(name = "SupplierId", nullable = false)
  private Long supplierId;

  /** 供应商单号（可空）：供应商自己那张单上的号，对账时靠它对 */
  @Nationalized
  @Column(name = "SupplierOrderNumber", length = 100)
  @Nullable
  private String supplierOrderNumber;

  /** 下单时间 */
  @Column(name = "OrderTime", nullable = false)
  private LocalDateTime orderTime;

  /** 交期（可空）：供应商答应哪天到，收货排期靠它 */
  @Column(name = "DeliveryTime")
  @Nullable
  private LocalDateTime deliveryTime;

  /** 状态编码，取值见 {@link PurchaseOrderStatusEnum} */
  @Nationalized
  @Column(name = "Status", nullable = false, length = 50)
  private String status;

  /** 备注（可空） */
  @Nationalized
  @Column(name = "Remark", length = 500)
  @Nullable
  private String remark;

  /** 采购单行 */
  @OneToMany(
      fetch = FetchType.EAGER,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE},
      orphanRemoval = true)
  @JoinColumn(name = "PurchaseOrderId")
  @SQLRestriction("IsDeleted = 0")
  private List<PurchaseOrderLine> purchaseOrderLines = new ArrayList<>();

  /**
   * 构造器
   *
   * @param supplierId 供应商Id
   * @param supplierOrderNumber 供应商单号（可空）
   * @param orderTime 下单时间
   * @param deliveryTime 交期（可空）
   * @param remark 备注（可空）
   * @param number 单号（传空串表示由 OrderAddedEvent 的监听器后续分配）
   */
  PurchaseOrder(
      long supplierId,
      @Nullable String supplierOrderNumber,
      LocalDateTime orderTime,
      @Nullable LocalDateTime deliveryTime,
      @Nullable String remark,
      String number) {
    super(number);
    apply(supplierId, supplierOrderNumber, orderTime, deliveryTime, remark);
    this.status = PurchaseOrderStatusEnum.DRAFT.getCode();
  }

  /**
   * 流水号类型
   *
   * @return 采购单类型
   */
  @Override
  public SerialNumberTypeEnum getSerialNumberType() {
    return SerialNumberTypeEnum.PURCHASE_ORDER;
  }

  /**
   * 获取采购单行（只读）
   *
   * @return 不可变列表
   */
  public List<PurchaseOrderLine> getPurchaseOrderLines() {
    return Collections.unmodifiableList(purchaseOrderLines);
  }

  /**
   * 修改单据头（仅草稿可改）
   *
   * @param supplierId 供应商Id
   * @param supplierOrderNumber 供应商单号（可空）
   * @param orderTime 下单时间
   * @param deliveryTime 交期（可空）
   * @param remark 备注（可空）
   * @throws DomainException 非草稿状态时抛出
   */
  public void modify(
      long supplierId,
      @Nullable String supplierOrderNumber,
      LocalDateTime orderTime,
      @Nullable LocalDateTime deliveryTime,
      @Nullable String remark) {
    ensureDraft();
    apply(supplierId, supplierOrderNumber, orderTime, deliveryTime, remark);
  }

  /**
   * 添加采购单行（仅草稿可加）
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
   * @throws DomainException 非草稿状态、或该物料已在本单中时抛出
   */
  public void addLine(
      long materialId,
      long variantId,
      @Nullable String supplierMaterialCode,
      BigDecimal qty,
      long unitId,
      BigDecimal price,
      BigDecimal discount,
      @Nullable Long taxRateId,
      @Nullable BigDecimal taxRate) {
    ensureDraft();
    if (findLineByVariantId(variantId) != null) {
      throw new DomainException("物料" + variantId + "已在本单中!");
    }
    purchaseOrderLines.add(
        new PurchaseOrderLine(
            materialId,
            variantId,
            supplierMaterialCode,
            qty,
            unitId,
            price,
            discount,
            taxRateId,
            taxRate));
  }

  /**
   * 修改采购单行（仅草稿可改）
   *
   * @param purchaseOrderLineId 采购单行Id
   * @param materialId 物料SPU
   * @param variantId 物料SKU
   * @param supplierMaterialCode 供应商料号（可空）
   * @param qty 数量
   * @param unitId 单位Id
   * @param price 单价（不含税）
   * @param discount 折扣（%）
   * @param taxRateId 税率Id（可空）
   * @param taxRate 税率快照（百分比，可空）
   * @throws DomainException 非草稿状态、行不存在、或该物料已在本单中时抛出
   */
  public void modifyLine(
      long purchaseOrderLineId,
      long materialId,
      long variantId,
      @Nullable String supplierMaterialCode,
      BigDecimal qty,
      long unitId,
      BigDecimal price,
      BigDecimal discount,
      @Nullable Long taxRateId,
      @Nullable BigDecimal taxRate) {
    ensureDraft();
    PurchaseOrderLine line = requireLine(purchaseOrderLineId);
    if (findLineByVariantId(variantId, purchaseOrderLineId) != null) {
      throw new DomainException("物料" + variantId + "已在本单中!");
    }
    line.modify(
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

  /**
   * 删除采购单行（仅草稿可删，逻辑删除）
   *
   * @param purchaseOrderLineId 采购单行Id
   * @throws DomainException 非草稿状态时抛出
   */
  public void deleteLine(long purchaseOrderLineId) {
    ensureDraft();
    PurchaseOrderLine line = findLine(purchaseOrderLineId);
    if (line == null) {
      return;
    }
    line.delete();
  }

  /**
   * 确认：单子算数了，供应商与价格就此定死，等着收货
   *
   * <p>收货本身不在这里：收了没有、入到哪个库位由库存模块开收货单做，本方法只管自己的状态。一张没有行的单
   * 确认过去也没东西可收，所以先拦在这里。
   *
   * @throws DomainException 非草稿状态、或单内没有行时抛出
   */
  public void confirm() {
    ensureDraft();
    if (activeLines().isEmpty()) {
      throw new DomainException("采购单没有行，不能确认");
    }
    this.status = PurchaseOrderStatusEnum.CONFIRMED.getCode();
  }

  /**
   * 收货：把本次到的货记到某一行的已收数量上
   *
   * <p>一张采购单能分多次收，每次收哪几行、各多少由收货单定；这里只把数量累加到行上，「收完没有」照样算得出来。
   * 入到哪个库位不在这里——那是收货单行上的事。
   *
   * @param purchaseOrderLineId 采购单行Id
   * @param qty 本次收货数量
   * @throws DomainException 单据未确认或已取消、行不存在、或累计收货超过订单量时抛出
   */
  public void receive(long purchaseOrderLineId, BigDecimal qty) {
    ensureReceivable();
    requireLine(purchaseOrderLineId).receive(qty);
  }

  /**
   * 关闭：结案，不再允许变更
   *
   * <p>与取消的区别：取消是「不买了」，关闭是「收过的照算、剩下的不要了」。短交就是走这条路——供应商只到了一部分，
   * 剩余额度一并作罢，但已到的那部分照常入库对账。
   *
   * @throws DomainException 已取消或已关闭时抛出
   */
  public void close() {
    if (isCancelled()) {
      throw new DomainException("采购单已取消，不能再关闭");
    }
    if (isClosed()) {
      throw new DomainException("采购单已关闭");
    }
    this.status = PurchaseOrderStatusEnum.CLOSED.getCode();
  }

  /**
   * 取消
   *
   * <p>已确认的单子可能已经有在途的收货单，取消前要由应用层先处理那些单——本方法只管本单。
   *
   * @throws DomainException 已取消或已关闭时抛出
   */
  public void cancel() {
    if (isCancelled()) {
      throw new DomainException("采购单已取消");
    }
    if (isClosed()) {
      throw new DomainException("采购单已关闭，不能再取消");
    }
    this.status = PurchaseOrderStatusEnum.CANCELLED.getCode();
  }

  /**
   * 是否草稿
   *
   * @return true 表示草稿
   */
  public boolean isDraft() {
    return PurchaseOrderStatusEnum.DRAFT.getCode().equals(this.status);
  }

  /**
   * 是否已确认
   *
   * @return true 表示已确认
   */
  public boolean isConfirmed() {
    return PurchaseOrderStatusEnum.CONFIRMED.getCode().equals(this.status);
  }

  /**
   * 是否已关闭
   *
   * @return true 表示已关闭
   */
  public boolean isClosed() {
    return PurchaseOrderStatusEnum.CLOSED.getCode().equals(this.status);
  }

  /**
   * 是否已取消
   *
   * @return true 表示已取消
   */
  public boolean isCancelled() {
    return PurchaseOrderStatusEnum.CANCELLED.getCode().equals(this.status);
  }

  // ==================== 私有：校验与赋值 ====================

  /**
   * 统一的校验与赋值入口（构造器与修改共用）
   *
   * @param supplierId 供应商Id
   * @param supplierOrderNumber 供应商单号（可空）
   * @param orderTime 下单时间
   * @param deliveryTime 交期（可空）
   * @param remark 备注（可空）
   * @throws DomainException 供应商非法，或交期早于下单时间时抛出
   */
  private void apply(
      long supplierId,
      @Nullable String supplierOrderNumber,
      LocalDateTime orderTime,
      @Nullable LocalDateTime deliveryTime,
      @Nullable String remark) {
    if (supplierId <= 0) {
      throw new DomainException("供应商不能为空");
    }
    if (deliveryTime != null && deliveryTime.isBefore(orderTime)) {
      throw new DomainException("交期不能早于下单时间");
    }
    this.supplierId = supplierId;
    this.supplierOrderNumber = StringUtils.trimToNull(supplierOrderNumber);
    this.orderTime = orderTime;
    this.deliveryTime = deliveryTime;
    this.remark = remark;
  }

  /**
   * 校验单据仍处于草稿
   *
   * @throws DomainException 非草稿状态时抛出
   */
  private void ensureDraft() {
    if (!isDraft()) {
      throw new DomainException("采购单已确认，不能再修改");
    }
  }

  /**
   * 校验单据允许收货
   *
   * <p>草稿还没确认、取消了的不要了，这两种都不该再往行上累加已收数量。
   *
   * @throws DomainException 单据未确认或已取消时抛出
   */
  private void ensureReceivable() {
    if (isDraft()) {
      throw new DomainException("采购单未确认，不能收货");
    }
    if (isCancelled()) {
      throw new DomainException("采购单已取消，不能收货");
    }
    if (isClosed()) {
      throw new DomainException("采购单已关闭，不能收货");
    }
  }

  /**
   * 按行Id查找未删除的采购单行
   *
   * @param purchaseOrderLineId 采购单行Id
   * @return 匹配到的采购单行，找不到返回 {@code null}
   */
  @Nullable
  private PurchaseOrderLine findLine(long purchaseOrderLineId) {
    return purchaseOrderLines.stream()
        .filter(l -> Objects.equals(l.getId(), purchaseOrderLineId) && !l.getIsDeleted())
        .findFirst()
        .orElse(null);
  }

  /**
   * 按物料SKU查找未删除的采购单行
   *
   * <p>同一张单里同一个 SKU 只允许出现一次：采购单按 SKU 归行，重复记账时收货、对账都说不清该核到哪一行。
   *
   * @param variantId 物料SKU
   * @param excludeId 排除的行Id（可空，新增时传 null）
   * @return 匹配到的采购单行，找不到返回 {@code null}
   */
  @Nullable
  private PurchaseOrderLine findLineByVariantId(long variantId, @Nullable Long excludeId) {
    return purchaseOrderLines.stream()
        .filter(l -> Objects.equals(l.getVariantId(), variantId) && !l.getIsDeleted())
        .filter(l -> !Objects.equals(l.getId(), excludeId))
        .findFirst()
        .orElse(null);
  }

  /**
   * 按物料SKU查找未删除的采购单行
   *
   * @param variantId 物料SKU
   * @return 匹配到的采购单行，找不到返回 {@code null}
   */
  @Nullable
  private PurchaseOrderLine findLineByVariantId(long variantId) {
    return findLineByVariantId(variantId, null);
  }

  /**
   * 取采购单行，不存在则抛异常
   *
   * @param purchaseOrderLineId 采购单行Id
   * @return 采购单行
   * @throws DomainException 采购单行不存在时抛出
   */
  private PurchaseOrderLine requireLine(long purchaseOrderLineId) {
    PurchaseOrderLine line = findLine(purchaseOrderLineId);
    if (line == null) {
      throw new DomainException("采购单行不存在!");
    }
    return line;
  }

  /**
   * 未删除的采购单行
   *
   * @return 采购单行列表
   */
  private List<PurchaseOrderLine> activeLines() {
    return purchaseOrderLines.stream().filter(l -> !l.getIsDeleted()).toList();
  }
}
