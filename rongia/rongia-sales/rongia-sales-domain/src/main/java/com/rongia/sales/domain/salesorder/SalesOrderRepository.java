package com.rongia.sales.domain.salesorder;

import com.rongia.shared.domain.repository.Repository;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 销售订单仓储接口 */
@NullMarked
public interface SalesOrderRepository extends Repository<SalesOrder> {

  /**
   * 按单号查询
   *
   * @param number 销售订单号
   * @return 销售订单（可空），不存在返回 null
   */
  @Nullable
  SalesOrder getByNumber(String number);

  /**
   * 按客户查询销售订单
   *
   * @param customerId 客户Id
   * @return 销售订单列表
   */
  List<SalesOrder> getListByCustomerId(long customerId);

  /**
   * 判断单号是否存在
   *
   * @param number 销售订单号
   * @param id 排除的销售订单Id（可空，新增时传 null）
   * @return true 表示已存在
   */
  boolean isExistByNumberExcludeId(String number, @Nullable Long id);

  /**
   * 判断单号是否存在
   *
   * @param number 销售订单号
   * @return true 表示已存在
   */
  default boolean isExistByNumberExcludeId(String number) {
    return isExistByNumberExcludeId(number, null);
  }
}
