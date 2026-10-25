package com.rongia.production.domain.productionreceipt;

import com.rongia.shared.domain.exception.DomainException;
import java.math.BigDecimal;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 生产入库单工厂，负责创建入库单 */
@NullMarked
public class ProductionReceiptFactory {

  /** 生产入库单仓储 */
  private final ProductionReceiptRepository productionReceiptRepository;

  /**
   * 构造器注入
   *
   * @param productionReceiptRepository 生产入库单仓储
   */
  public ProductionReceiptFactory(ProductionReceiptRepository productionReceiptRepository) {
    this.productionReceiptRepository = productionReceiptRepository;
  }

  /**
   * 创建生产入库单
   *
   * @param workOrderId 工单Id
   * @param workOrderNumber 工单号（快照）
   * @param materialId 产出SPU（快照）
   * @param variantId 产出SKU（快照）
   * @param unitId 单位Id（快照）
   * @param warehouseId 入库仓库Id
   * @param appliedQty 申报入库数量
   * @param remark 备注（可空）
   * @param number 入库单号（传空串表示由 OrderAddedEvent 的监听器分配）
   * @return 生产入库单聚合
   * @throws DomainException 单号已存在时抛出
   */
  public ProductionReceipt create(
      long workOrderId,
      String workOrderNumber,
      long materialId,
      long variantId,
      long unitId,
      long warehouseId,
      BigDecimal appliedQty,
      @Nullable String remark,
      String number) {
    // 单号非空时才校验唯一，空串代表尚未分配
    if (!number.isBlank() && productionReceiptRepository.isExistByNumberExcludeId(number)) {
      throw new DomainException("入库单号" + number + "已存在!");
    }
    return new ProductionReceipt(
        workOrderId,
        workOrderNumber,
        materialId,
        variantId,
        unitId,
        warehouseId,
        appliedQty,
        remark,
        number);
  }
}
