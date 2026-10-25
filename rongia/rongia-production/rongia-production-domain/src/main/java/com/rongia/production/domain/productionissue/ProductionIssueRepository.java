package com.rongia.production.domain.productionissue;

import com.rongia.shared.domain.repository.Repository;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 生产领料单仓储接口 */
@NullMarked
public interface ProductionIssueRepository extends Repository<ProductionIssue> {

  /**
   * 按单号查询
   *
   * @param number 生产领料单号
   * @return 生产领料单（可空），不存在返回 null
   */
  @Nullable ProductionIssue getByNumber(String number);

  /**
   * 按工单查询生产领料单
   *
   * <p>一张工单会领很多次，返回列表。
   *
   * @param workOrderId 工单Id
   * @return 生产领料单列表
   */
  List<ProductionIssue> getListByWorkOrderId(long workOrderId);

  /**
   * 判断单号是否存在
   *
   * @param number 生产领料单号
   * @param id 排除的生产领料单Id（可空，新增时传 null）
   * @return true 表示已存在
   */
  boolean isExistByNumberExcludeId(String number, @Nullable Long id);

  /**
   * 判断单号是否存在
   *
   * @param number 生产领料单号
   * @return true 表示已存在
   */
  default boolean isExistByNumberExcludeId(String number) {
    return isExistByNumberExcludeId(number, null);
  }
}
