package com.rongia.production.domain.workcenter;

import com.rongia.shared.domain.repository.Repository;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 工作中心仓储接口 */
@NullMarked
public interface WorkCenterRepository extends Repository<WorkCenter> {

  /**
   * 按名称查询
   *
   * @param name 工作中心名称
   * @return 工作中心（可空），不存在返回 null
   */
  @Nullable WorkCenter getByName(String name);

  /**
   * 判断工作中心名称是否存在
   *
   * @param name 工作中心名称
   * @param id 排除的工作中心Id（可空，修改时传自己）
   * @return true 表示已存在
   */
  boolean isExistByNameExcludeId(String name, @Nullable Long id);

  /**
   * 判断工作中心名称是否存在
   *
   * @param name 工作中心名称
   * @return true 表示已存在
   */
  default boolean isExistByNameExcludeId(String name) {
    return isExistByNameExcludeId(name, null);
  }
}
