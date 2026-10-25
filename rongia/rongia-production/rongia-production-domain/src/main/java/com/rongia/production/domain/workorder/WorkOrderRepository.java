package com.rongia.production.domain.workorder;

import com.rongia.shared.domain.repository.Repository;
import java.time.LocalDateTime;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 工单仓储接口 */
@NullMarked
public interface WorkOrderRepository extends Repository<WorkOrder> {

  /**
   * 按单号查询
   *
   * @param number 工单号
   * @return 工单（可空），不存在返回 null
   */
  @Nullable WorkOrder getByNumber(String number);

  /**
   * 按来源单据查询工单
   *
   * <p>销售订单行拆批投产时，一个来源会对应多张工单，所以返回列表。
   *
   * @param sourceType 来源类型编码
   * @param sourceId 来源单据Id
   * @return 工单列表
   */
  List<WorkOrder> getListBySource(String sourceType, long sourceId);

  /**
   * 按产出SKU查询工单
   *
   * @param variantId 产出SKU
   * @return 工单列表
   */
  List<WorkOrder> getListByVariantId(long variantId);

  /**
   * 判断单号是否存在
   *
   * @param number 工单号
   * @param id 排除的工单Id（可空，新增时传 null）
   * @return true 表示已存在
   */
  boolean isExistByNumberExcludeId(String number, @Nullable Long id);

  /**
   * 判断单号是否存在
   *
   * @param number 工单号
   * @return true 表示已存在
   */
  default boolean isExistByNumberExcludeId(String number) {
    return isExistByNumberExcludeId(number, null);
  }
}
