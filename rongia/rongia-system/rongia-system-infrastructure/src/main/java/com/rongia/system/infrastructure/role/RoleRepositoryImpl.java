package com.rongia.system.infrastructure.role;

import com.rongia.system.domain.role.Role;
import com.rongia.system.domain.role.RoleRepository;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

/** 角色仓储实现 */
@NullMarked
@Repository
public class RoleRepositoryImpl implements RoleRepository {

  /** JPA 接口 */
  private final RoleJpa jpa;

  /**
   * 构造器注入
   *
   * @param jpa JPA 接口
   */
  public RoleRepositoryImpl(RoleJpa jpa) {
    this.jpa = jpa;
  }

  /**
   * 按主键查询
   *
   * @param id 主键
   * @return 角色，不存在返回 null
   */
  @Override
  @Nullable
  public Role get(long id) {
    return jpa.findById(id).orElse(null);
  }

  /**
   * 按角色名称查询
   *
   * @param name 角色名称
   * @return 角色，不存在返回 null
   */
  @Override
  @Nullable
  public Role getByName(String name) {
    return jpa.findByName(name).orElse(null);
  }

  /**
   * 判断角色名称是否已存在（可排除自身）
   *
   * @param name 角色名称
   * @param id 排除的角色Id（可空）
   * @return 是否存在
   */
  @Override
  public boolean isExistByNameExcludeId(String name, @Nullable Long id) {
    return jpa.existsByNameExcludeId(name, id);
  }

  /**
   * 保存（新增或更新）
   *
   * @param model 角色聚合
   */
  @Override
  public void save(Role model) {
    jpa.save(model);
  }
}
