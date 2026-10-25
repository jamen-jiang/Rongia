package com.rongia.production.domain.productionreturn;

import com.rongia.shared.domain.exception.DomainException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 生产退料单工厂，负责创建生产退料单 */
@NullMarked
public class ProductionReturnFactory {

  /** 生产退料单仓储 */
  private final ProductionReturnRepository productionReturnRepository;

  /**
   * 构造器注入
   *
   * @param productionReturnRepository 生产退料单仓储
   */
  public ProductionReturnFactory(ProductionReturnRepository productionReturnRepository) {
    this.productionReturnRepository = productionReturnRepository;
  }

  /**
   * 创建生产退料单
   *
   * @param workOrderId 工单Id
   * @param workOrderNumber 工单号（冗余，便于展示）
   * @param operatorId 经办人Id（可空）
   * @param remark 备注（可空）
   * @param number 退料单号（传空串表示由 OrderAddedEvent 的监听器分配）
   * @return 生产退料单聚合
   * @throws DomainException 单号已存在时抛出
   */
  public ProductionReturn create(
      long workOrderId,
      String workOrderNumber,
      @Nullable Long operatorId,
      @Nullable String remark,
      String number) {
    // 单号非空时才校验唯一，空串代表尚未分配
    if (!number.isBlank() && productionReturnRepository.isExistByNumberExcludeId(number)) {
      throw new DomainException("退料单号" + number + "已存在!");
    }
    return new ProductionReturn(workOrderId, workOrderNumber, operatorId, remark, number);
  }
}
