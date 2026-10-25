package com.rongia.system.domain.role;

import com.rongia.shared.domain.repository.Repository;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 角色仓储接口 */
@NullMarked
public interface RoleRepository extends Repository<Role> {

  /**
   * 按角色名称查询
   *
   * @param name 角色名称
   * @return 角色（可空），不存在返回 null
   */
  @Nullable Role getByName(String name);

  /**
   * 判断角色名称是否存在
   *
   * @param name 角色名称
   * @param id 排除的角色Id（可空，新增时传 null）
   * @return true 表示已存在
   */
  boolean isExistByNameExcludeId(String name, @Nullable Long id);

  /**
   * 判断角色名称是否存在（新增时校验，无需排除Id）
   *
   * @param name 角色名称
   * @return true 表示已存在
   */
  default boolean isExistByNameExcludeId(String name) {
    return isExistByNameExcludeId(name, null);
  }
}
