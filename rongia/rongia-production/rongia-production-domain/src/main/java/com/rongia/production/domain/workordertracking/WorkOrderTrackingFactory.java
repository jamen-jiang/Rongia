package com.rongia.production.domain.workordertracking;

import java.math.BigDecimal;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 工单跟踪（报工单）工厂，负责创建报工单 */
@NullMarked
public class WorkOrderTrackingFactory {

  /**
   * 创建报工单
   *
   * @param workOrderId 工单Id
   * @param workOrderNumber 工单号（快照）
   * @param workOrderOperationId 工单工序Id
   * @param operationId 工序Id（快照）
   * @param operationName 工序名称（快照）
   * @param workCenterId 工作中心Id（快照）
   * @param workCenterName 工作中心名称（快照）
   * @param workTime 本次工时（分钟）
   * @param goodQty 本次良品数量
   * @param lossQty 本次报废数量
   * @param remark 备注（可空）
   * @return 报工单聚合
   */
  public WorkOrderTracking create(
      long workOrderId,
      String workOrderNumber,
      long workOrderOperationId,
      long operationId,
      String operationName,
      long workCenterId,
      String workCenterName,
      BigDecimal workTime,
      BigDecimal goodQty,
      BigDecimal lossQty,
      @Nullable String remark) {
    return new WorkOrderTracking(
        workOrderId,
        workOrderNumber,
        workOrderOperationId,
        operationId,
        operationName,
        workCenterId,
        workCenterName,
        workTime,
        goodQty,
        lossQty,
        remark);
  }
}
