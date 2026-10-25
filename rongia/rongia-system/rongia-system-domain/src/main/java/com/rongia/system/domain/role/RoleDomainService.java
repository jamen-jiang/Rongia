package com.rongia.system.domain.role;

import com.rongia.shared.domain.exception.DomainException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 角色领域服务 */
@NullMarked
public class RoleDomainService {

  /** 角色仓储 */
  private final RoleRepository roleRepository;

  /**
   * 构造器注入
   *
   * @param roleRepository 角色仓储
   */
  public RoleDomainService(RoleRepository roleRepository) {

    this.roleRepository = roleRepository;
  }

  /**
   * 修改角色（按Id查询后修改）
   *
   * @param id 角色Id
   * @param name 角色名称
   * @param remark 备注（可空）
   * @return 角色聚合
   * @throws DomainException 角色不存在时抛出
   * @throws DomainException 角色名称已存在时抛出
   */
  public Role modify(long id, String name, @Nullable String remark) {
    // 1. 查角色
    Role model = roleRepository.get(id);
    if (model == null) {
      throw new DomainException("角色不存在!");
    }
    // 2. 委托给使用已有聚合的重载
    modify(model, name, remark);
    return model;
  }

  /**
   * 修改角色（使用已有聚合，避免重复查询）
   *
   * @param role 角色聚合
   * @param name 角色名称
   * @param remark 备注（可空）
   * @throws DomainException 角色名称已存在时抛出
   */
  public void modify(Role role, String name, @Nullable String remark) {
    // 校验角色名称唯一（排除自身）
    if (roleRepository.isExistByNameExcludeId(name, role.getId())) {
      throw new DomainException("角色" + name + "已存在!");
    }
    role.modify(name, remark);
  }
}
