package com.rongia.production.domain.jobcard;

import com.rongia.shared.domain.repository.Repository;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 工卡仓储接口 */
@NullMarked
public interface JobCardRepository extends Repository<JobCard> {

  /**
   * 按工单工序取当前那张工卡
   *
   * <p>一道工序同时只有一张有效的卡：报工时先找卡，找不到（或只剩已取消的卡）才新建一张。
   * 已取消的卡不返回。
   *
   * @param workOrderOperationId 工单工序Id
   * @return 工卡（可空），没有有效卡时返回 null
   */
  @Nullable JobCard getByWorkOrderOperationId(long workOrderOperationId);

  /**
   * 判断单号是否存在
   *
   * @param number 工卡单号
   * @return true 表示已存在
   */
  boolean isExistByNumberExcludeId(String number);
}
