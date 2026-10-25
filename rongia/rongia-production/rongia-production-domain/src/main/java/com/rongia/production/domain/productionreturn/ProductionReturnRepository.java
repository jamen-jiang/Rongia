package com.rongia.production.domain.productionreturn;

import com.rongia.shared.domain.repository.Repository;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 生产退料单仓储接口 */
@NullMarked
public interface ProductionReturnRepository extends Repository<ProductionReturn> {

  /**
   * 按单号查询
   *
   * @param number 退料单号
   * @return 生产退料单（可空），不存在返回 null
   */
  @Nullable ProductionReturn getByNumber(String number);

  /**
   * 按工单查询退料单
   *
   * <p>一张工单会退很多次，返回列表。
   *
   * @param workOrderId 工单Id
   * @return 生产退料单列表
   */
  List<ProductionReturn> getListByWorkOrderId(long workOrderId);

  /**
   * 判断单号是否存在
   *
   * @param number 退料单号
   * @param id 排除的退料单Id（可空，新增时传 null）
   * @return true 表示已存在
   */
  boolean isExistByNumberExcludeId(String number, @Nullable Long id);

  /**
   * 判断单号是否存在
   *
   * @param number 退料单号
   * @return true 表示已存在
   */
  default boolean isExistByNumberExcludeId(String number) {
    return isExistByNumberExcludeId(number, null);
  }
}
