package com.rongia.production.domain.productionreceipt;

import com.rongia.shared.domain.repository.Repository;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 生产入库单仓储接口 */
@NullMarked
public interface ProductionReceiptRepository extends Repository<ProductionReceipt> {

  /**
   * 按单号查询
   *
   * @param number 入库单号
   * @return 入库单（可空），不存在返回 null
   */
  @Nullable ProductionReceipt getByNumber(String number);

  /**
   * 按工单查询入库单
   *
   * <p>一张工单可以分几次入库，返回列表；工单上的已入库数量就是这些单子的合计。
   *
   * @param workOrderId 工单Id
   * @return 入库单列表
   */
  List<ProductionReceipt> getListByWorkOrderId(long workOrderId);

  /**
   * 判断单号是否存在
   *
   * @param number 入库单号
   * @param id 排除的入库单Id（可空，新增时传 null）
   * @return true 表示已存在
   */
  boolean isExistByNumberExcludeId(String number, @Nullable Long id);

  /**
   * 判断单号是否存在
   *
   * @param number 入库单号
   * @return true 表示已存在
   */
  default boolean isExistByNumberExcludeId(String number) {
    return isExistByNumberExcludeId(number, null);
  }
}
