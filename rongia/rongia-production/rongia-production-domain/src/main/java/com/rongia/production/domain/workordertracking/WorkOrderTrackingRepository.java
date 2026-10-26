package com.rongia.production.domain.workordertracking;

import com.rongia.shared.domain.repository.Repository;
import java.util.List;
import org.jspecify.annotations.NullMarked;

/** 工单跟踪（报工单）仓储接口 */
@NullMarked
public interface WorkOrderTrackingRepository extends Repository<WorkOrderTracking> {

  /**
   * 按工单查询报工单
   *
   * @param workOrderId 工单Id
   * @return 报工单列表
   */
  List<WorkOrderTracking> getListByWorkOrderId(long workOrderId);

  /**
   * 按工单工序查询报工单
   *
   * @param workOrderOperationId 工单工序Id
   * @return 报工单列表
   */
  List<WorkOrderTracking> getListByWorkOrderOperationId(long workOrderOperationId);
}
