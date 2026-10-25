package com.rongia.sales.domain.salesorder;

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
 * 销售订单
 *
 * <p>客户下单的那张纸：谁要的、什么时候要、要哪几个 SKU、各多少、什么价。一个客户一张单，一个交期——镜片按单
 * 排产，同一张单基本同一天要货；真要分两批交，是拆成两张单的事，不是在一张单的行上挂不同日期。
 *
 * <p>金额三列（未税、税额、含税）跟着行算完就落库：税是按行取整的，单头只是把行加起来，每次现算未必算出同一个数， 打印给客户看的也得是当初那一版。
 *
 * <p>生命周期五态：{@code DRAFT → TO_DELIVER → COMPLETED}，任意未结案阶段都能 {@code CANCELLED}，做过的单子 剩下的不要了走 {@code
 * CLOSED}。不做「已确认」那一档：草稿一交出去就是待发货，中间没有「批了但还没安排」的 空档——镜片按单排产，确认那一下就把工单派出去了。
 *
 * <p>单上不挂 BOM、不挂工艺路线、不挂工单号：那是车间的事。确认后按行开工单由应用层编排，一行一张工单，工单靠 来源单据找回这张单。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "SalesOrder",
    indexes = {
      @Index(name = "IX_SalesOrder_CustomerId", columnList = "CustomerId"),
      @Index(name = "IX_SalesOrder_OrderTime", columnList = "OrderTime"),
      @Index(name = "IX_SalesOrder_Status", columnList = "Status")
    })
public class SalesOrder extends OrderAggregateRoot {

  /** 客户Id */
  @Column(name = "CustomerId", nullable = false)
  private Long customerId;

  /** 客户单号（可空）：客户自己那张采购单上的号，对账时靠它对 */
  @Nationalized
  @Column(name = "CustomerOrderNumber", length = 100)
  @Nullable
  private String customerOrderNumber;

  /** 下单时间 */
  @Column(name = "OrderTime", nullable = false)
  private LocalDateTime orderTime;

  /** 交期（可空）：答应客户哪天交货，往下就是工单的交期 */
  @Column(name = "DeliveryTime")
  @Nullable
  private LocalDateTime deliveryTime;

  /** 状态编码，取值见 {@link SalesOrderStatusEnum} */
  @Nationalized
  @Column(name = "Status", nullable = false, length = 50)
  private String status;

  /** 备注（可空） */
  @Nationalized
  @Column(name = "Remark", length = 500)
  @Nullable
  private String remark;

  /** 销售订单行 */
  @OneToMany(
      fetch = FetchType.EAGER,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE},
      orphanRemoval = true)
  @JoinColumn(name = "SalesOrderId")
  @SQLRestriction("IsDeleted = 0")
  private List<SalesOrderLine> salesOrderLines = new ArrayList<>();

  /**
   * 构造器
   *
   * @param customerId 客户Id
   * @param customerOrderNumber 客户单号（可空）
   * @param orderTime 下单时间
   * @param deliveryTime 交期（可空）
   * @param remark 备注（可空）
   * @param number 单号（传空串表示由 OrderAddedEvent 的监听器后续分配）
   */
  SalesOrder(
      long customerId,
      @Nullable String customerOrderNumber,
      LocalDateTime orderTime,
      @Nullable LocalDateTime deliveryTime,
      @Nullable String remark,
      String number) {
    super(number);
    apply(customerId, customerOrderNumber, orderTime, deliveryTime, remark);
    this.status = SalesOrderStatusEnum.DRAFT.getCode();
  }

  /**
   * 流水号类型
   *
   * @return 销售订单类型
   */
  @Override
  public SerialNumberTypeEnum getSerialNumberType() {
    return SerialNumberTypeEnum.SALES_ORDER;
  }

  /**
   * 获取销售订单行（只读）
   *
   * @return 不可变列表
   */
  public List<SalesOrderLine> getSalesOrderLines() {
    return Collections.unmodifiableList(salesOrderLines);
  }

  /**
   * 修改单据头（仅草稿可改）
   *
   * @param customerId 客户Id
   * @param customerOrderNumber 客户单号（可空）
   * @param orderTime 下单时间
   * @param deliveryTime 交期（可空）
   * @param remark 备注（可空）
   * @throws DomainException 非草稿状态时抛出
   */
  public void modify(
      long customerId,
      @Nullable String customerOrderNumber,
      LocalDateTime orderTime,
      @Nullable LocalDateTime deliveryTime,
      @Nullable String remark) {
    ensureDraft();
    apply(customerId, customerOrderNumber, orderTime, deliveryTime, remark);
  }

  /**
   * 添加订单行（仅草稿可加）
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
   * @throws DomainException 非草稿状态、或行号重复时抛出
   */
  public void addLine(
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
    ensureDraft();
    if (findLineBySequence(sequence) != null) {
      throw new DomainException("销售订单行号" + sequence + "已存在!");
    }
    salesOrderLines.add(
        new SalesOrderLine(
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
            taxRate));
  }

  /**
   * 修改订单行（仅草稿可改）
   *
   * @param salesOrderLineId 销售订单行Id
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
   * @throws DomainException 非草稿状态、行不存在、或行号重复时抛出
   */
  public void modifyLine(
      long salesOrderLineId,
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
    ensureDraft();
    SalesOrderLine line = requireLine(salesOrderLineId);
    if (findLineBySequence(sequence, salesOrderLineId) != null) {
      throw new DomainException("销售订单行号" + sequence + "已存在!");
    }
    line.modify(
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

  /**
   * 删除订单行（仅草稿可删，逻辑删除）
   *
   * @param salesOrderLineId 销售订单行Id
   * @throws DomainException 非草稿状态时抛出
   */
  public void deleteLine(long salesOrderLineId) {
    ensureDraft();
    SalesOrderLine line = findLine(salesOrderLineId);
    if (line == null) {
      return;
    }
    line.delete();
  }

  /**
   * 转待发货：单子算数了，客户与价格就此定死，等着出货
   *
   * <p>按行派工单由应用层在同一个事务里编排，本方法只管自己的状态：一张没有行的订单转过去也没东西可做， 所以先拦在这里。
   *
   * @throws DomainException 非草稿状态、或单内没有行时抛出
   */
  public void deliver() {
    ensureDraft();
    if (activeLines().isEmpty()) {
      throw new DomainException("销售订单没有行，不能转待发货");
    }
    this.status = SalesOrderStatusEnum.TO_DELIVER.getCode();
  }

  /**
   * 完成：货全发完了，这单的活干完了
   *
   * @throws DomainException 非待发货状态时抛出
   */
  public void complete() {
    if (!isToDeliver()) {
      throw new DomainException("只有待发货的销售订单才能完成");
    }
    this.status = SalesOrderStatusEnum.COMPLETED.getCode();
  }

  /**
   * 关闭：结案，不再允许变更
   *
   * <p>与取消的区别：取消是「不做了」，关闭是「做过的照算、剩下的不要了」。短交就是走这条路——客户只收了 一部分，剩余额度一并作罢，但已发的那部分照常对账。
   *
   * @throws DomainException 已取消或已关闭时抛出
   */
  public void close() {
    if (isCancelled()) {
      throw new DomainException("销售订单已取消，不能再关闭");
    }
    if (isClosed()) {
      throw new DomainException("销售订单已关闭");
    }
    this.status = SalesOrderStatusEnum.CLOSED.getCode();
  }

  /**
   * 取消
   *
   * <p>转待发货的单子可能已经派了工单，取消前要由应用层先把那些工单取消掉——本方法只管本单。
   *
   * @throws DomainException 已取消或已关闭时抛出
   */
  public void cancel() {
    if (isCancelled()) {
      throw new DomainException("销售订单已取消");
    }
    if (isClosed()) {
      throw new DomainException("销售订单已关闭，不能再取消");
    }
    this.status = SalesOrderStatusEnum.CANCELLED.getCode();
  }

  /**
   * 是否草稿
   *
   * @return true 表示草稿
   */
  public boolean isDraft() {
    return SalesOrderStatusEnum.DRAFT.getCode().equals(this.status);
  }

  /**
   * 是否待发货
   *
   * @return true 表示待发货
   */
  public boolean isToDeliver() {
    return SalesOrderStatusEnum.TO_DELIVER.getCode().equals(this.status);
  }

  /**
   * 是否已完成
   *
   * @return true 表示已完成
   */
  public boolean isCompleted() {
    return SalesOrderStatusEnum.COMPLETED.getCode().equals(this.status);
  }

  /**
   * 是否已关闭
   *
   * @return true 表示已关闭
   */
  public boolean isClosed() {
    return SalesOrderStatusEnum.CLOSED.getCode().equals(this.status);
  }

  /**
   * 是否已取消
   *
   * @return true 表示已取消
   */
  public boolean isCancelled() {
    return SalesOrderStatusEnum.CANCELLED.getCode().equals(this.status);
  }

  // ==================== 私有：校验与赋值 ====================

  /**
   * 统一的校验与赋值入口（构造器与修改共用）
   *
   * @param customerId 客户Id
   * @param customerOrderNumber 客户单号（可空）
   * @param orderTime 下单时间
   * @param deliveryTime 交期（可空）
   * @param remark 备注（可空）
   * @throws DomainException 客户非法，或交期早于下单时间时抛出
   */
  private void apply(
      long customerId,
      @Nullable String customerOrderNumber,
      LocalDateTime orderTime,
      @Nullable LocalDateTime deliveryTime,
      @Nullable String remark) {
    if (customerId <= 0) {
      throw new DomainException("客户不能为空");
    }
    if (deliveryTime != null && deliveryTime.isBefore(orderTime)) {
      throw new DomainException("交期不能早于下单时间");
    }
    this.customerId = customerId;
    this.customerOrderNumber = StringUtils.trimToNull(customerOrderNumber);
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
      throw new DomainException("销售订单已提交，不能再修改");
    }
  }

  /**
   * 按行Id查找未删除的订单行
   *
   * @param salesOrderLineId 销售订单行Id
   * @return 匹配到的订单行，找不到返回 {@code null}
   */
  @Nullable
  private SalesOrderLine findLine(long salesOrderLineId) {
    return salesOrderLines.stream()
        .filter(l -> Objects.equals(l.getId(), salesOrderLineId) && !l.getIsDeleted())
        .findFirst()
        .orElse(null);
  }

  /**
   * 按行号查找未删除的订单行
   *
   * @param sequence 行号
   * @param excludeId 排除的行Id（可空，新增时传 null）
   * @return 匹配到的订单行，找不到返回 {@code null}
   */
  @Nullable
  private SalesOrderLine findLineBySequence(int sequence, @Nullable Long excludeId) {
    return salesOrderLines.stream()
        .filter(l -> Objects.equals(l.getSequence(), sequence) && !l.getIsDeleted())
        .filter(l -> !Objects.equals(l.getId(), excludeId))
        .findFirst()
        .orElse(null);
  }

  /**
   * 按行号查找未删除的订单行
   *
   * @param sequence 行号
   * @return 匹配到的订单行，找不到返回 {@code null}
   */
  @Nullable
  private SalesOrderLine findLineBySequence(int sequence) {
    return findLineBySequence(sequence, null);
  }

  /**
   * 取订单行，不存在则抛异常
   *
   * @param salesOrderLineId 销售订单行Id
   * @return 订单行
   * @throws DomainException 订单行不存在时抛出
   */
  private SalesOrderLine requireLine(long salesOrderLineId) {
    SalesOrderLine line = findLine(salesOrderLineId);
    if (line == null) {
      throw new DomainException("销售订单行不存在!");
    }
    return line;
  }

  /**
   * 未删除的订单行
   *
   * @return 订单行列表
   */
  private List<SalesOrderLine> activeLines() {
    return salesOrderLines.stream().filter(l -> !l.getIsDeleted()).toList();
  }
}
