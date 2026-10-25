package com.rongia.production.domain.productionreport;

import com.rongia.shared.domain.repository.Repository;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 生产报工单仓储接口 */
@NullMarked
public interface ProductionReportRepository extends Repository<ProductionReport> {

  /**
   * 按单号查询
   *
   * @param number 报工单号
   * @return 报工单（可空），不存在返回 null
   */
  @Nullable ProductionReport getByNumber(String number);

  /**
   * 按工单查询报工单
   *
   * @param workOrderId 工单Id
   * @return 报工单列表
   */
  List<ProductionReport> getListByWorkOrderId(long workOrderId);

  /**
   * 按工单工序查询报工单
   *
   * @param workOrderOperationId 工单工序Id
   * @return 报工单列表
   */
  List<ProductionReport> getListByWorkOrderOperationId(long workOrderOperationId);

  /**
   * 判断单号是否存在
   *
   * @param number 报工单号
   * @param id 排除的报工单Id（可空，新增时传 null）
   * @return true 表示已存在
   */
  boolean isExistByNumberExcludeId(String number, @Nullable Long id);

  /**
   * 判断单号是否存在
   *
   * @param number 报工单号
   * @return true 表示已存在
   */
  default boolean isExistByNumberExcludeId(String number) {
    return isExistByNumberExcludeId(number, null);
  }
}
