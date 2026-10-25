package com.rongia.sales.domain.salesorder;

import com.rongia.shared.domain.exception.DomainException;
import java.time.LocalDateTime;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 销售订单工厂，负责创建销售订单 */
@NullMarked
public class SalesOrderFactory {

  /** 销售订单仓储 */
  private final SalesOrderRepository salesOrderRepository;

  /**
   * 构造器注入
   *
   * @param salesOrderRepository 销售订单仓储
   */
  public SalesOrderFactory(SalesOrderRepository salesOrderRepository) {
    this.salesOrderRepository = salesOrderRepository;
  }

  /**
   * 创建销售订单
   *
   * @param customerId 客户Id
   * @param customerOrderNumber 客户单号（可空）
   * @param orderTime 下单时间
   * @param deliveryTime 交期（可空）
   * @param remark 备注（可空）
   * @param number 销售订单号（传空串表示由 OrderAddedEvent 的监听器分配）
   * @return 销售订单聚合
   * @throws DomainException 单号已存在时抛出
   */
  public SalesOrder create(
      long customerId,
      @Nullable String customerOrderNumber,
      LocalDateTime orderTime,
      @Nullable LocalDateTime deliveryTime,
      @Nullable String remark,
      String number) {
    // 单号非空时才校验唯一，空串代表尚未分配
    if (!number.isBlank() && salesOrderRepository.isExistByNumberExcludeId(number)) {
      throw new DomainException("销售订单号" + number + "已存在!");
    }
    return new SalesOrder(
        customerId, customerOrderNumber, orderTime, deliveryTime, remark, number);
  }
}
