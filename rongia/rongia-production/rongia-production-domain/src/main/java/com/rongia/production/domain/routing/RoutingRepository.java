package com.rongia.production.domain.routing;

import com.rongia.shared.domain.repository.Repository;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 工艺路线仓储接口 */
@NullMarked
public interface RoutingRepository extends Repository<Routing> {

  /**
   * 按名称查询
   *
   * @param name 路线名称
   * @return 工艺路线（可空），不存在返回 null
   */
  @Nullable Routing getByName(String name);

  /**
   * 判断名称是否已被占用
   *
   * @param name 路线名称
   * @param id 排除的工艺路线Id（可空，新增时传 null）
   * @return true 表示已存在
   */
  boolean isExistByNameExcludeId(String name, @Nullable Long id);

  /**
   * 判断名称是否已被占用
   *
   * @param name 路线名称
   * @return true 表示已存在
   */
  default boolean isExistByNameExcludeId(String name) {
    return isExistByNameExcludeId(name, null);
  }
}
