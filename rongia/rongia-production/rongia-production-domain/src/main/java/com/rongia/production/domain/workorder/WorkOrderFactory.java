package com.rongia.production.domain.workorder;

import com.rongia.shared.domain.exception.DomainException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 工单工厂，负责创建工单 */
@NullMarked
public class WorkOrderFactory {

  /** 工单仓储 */
  private final WorkOrderRepository workOrderRepository;

  /**
   * 构造器注入
   *
   * @param workOrderRepository 工单仓储
   */
  public WorkOrderFactory(WorkOrderRepository workOrderRepository) {
    this.workOrderRepository = workOrderRepository;
  }

  /**
   * 创建工单
   *
   * @param sourceType 来源类型编码
   * @param sourceId 来源单据Id（可空，库存补货时为空）
   * @param sourceNumber 来源单号（可空，冗余存储便于追溯与展示）
   * @param bomId 指定BOM（可空，为空时下达自动取生效版本）
   * @param routingId 指定工艺路线（可空，为空时下达自动取生效版本）
   * @param materialId 产出SPU
   * @param variantId 产出SKU
   * @param deliveryTime 交期（可空，承诺的完工时间，生产侧只读不写）
   * @param startTime 开工时间（可空，排产时填计划，照 Odoo 的 date_start 语义)
   * @param qty 计划数量
   * @param unitId 单位Id
   * @param srcLocationId 发料库位Id
   * @param destLocationId 成品收货库位Id

   * @param remark 备注（可空）
   * @param number 工单号（传空串表示由 OrderAddedEvent 的监听器分配）
   * @return 工单聚合
   * @throws DomainException 单号已存在时抛出
   */
  public WorkOrder create(
      String sourceType,
      @Nullable Long sourceId,
      @Nullable String sourceNumber,
      @Nullable Long bomId,
      @Nullable Long routingId,
      long materialId,
      long variantId,
      @Nullable LocalDateTime deliveryTime,
      @Nullable LocalDateTime startTime,
      BigDecimal qty,
      long unitId,
      long srcLocationId,
      long destLocationId,
      @Nullable String remark,
      String number) {
    // 单号非空时才校验唯一，空串代表尚未分配
    if (!number.isBlank() && workOrderRepository.isExistByNumberExcludeId(number)) {
      throw new DomainException("工单号" + number + "已存在!");
    }
    return new WorkOrder(
        sourceType,
        sourceId,
        sourceNumber,
        bomId,
        routingId,
        materialId,
        variantId,
        deliveryTime,
        startTime,
        qty,
        unitId,
        srcLocationId,
        destLocationId,
        remark,
        number);
  }
}
