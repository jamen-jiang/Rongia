package com.rongia.purchase.domain.purchaseorder;

import com.rongia.shared.domain.exception.DomainException;
import java.time.LocalDateTime;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 采购单工厂，负责创建采购单 */
@NullMarked
public class PurchaseOrderFactory {

  /** 采购单仓储 */
  private final PurchaseOrderRepository purchaseOrderRepository;

  /**
   * 构造器注入
   *
   * @param purchaseOrderRepository 采购单仓储
   */
  public PurchaseOrderFactory(PurchaseOrderRepository purchaseOrderRepository) {
    this.purchaseOrderRepository = purchaseOrderRepository;
  }

  /**
   * 创建采购单
   *
   * @param supplierId 供应商Id
   * @param supplierOrderNumber 供应商单号（可空）
   * @param orderTime 下单时间
   * @param deliveryTime 交期（可空）
   * @param remark 备注（可空）
   * @param number 采购单号（传空串表示由 OrderAddedEvent 的监听器分配）
   * @return 采购单聚合
   * @throws DomainException 单号已存在时抛出
   */
  public PurchaseOrder create(
      long supplierId,
      @Nullable String supplierOrderNumber,
      LocalDateTime orderTime,
      @Nullable LocalDateTime deliveryTime,
      @Nullable String remark,
      String number) {
    // 单号非空时才校验唯一，空串代表尚未分配
    if (!number.isBlank() && purchaseOrderRepository.isExistByNumberExcludeId(number)) {
      throw new DomainException("采购单号" + number + "已存在!");
    }
    return new PurchaseOrder(
        supplierId,
        supplierOrderNumber,
        orderTime,
        deliveryTime,
        remark,
        number);
  }
}
