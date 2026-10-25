package com.rongia.system.domain.role;

import com.rongia.shared.domain.exception.DomainException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 角色工厂，负责创建角色 */
@NullMarked
public class RoleFactory {

  /** 角色仓储 */
  private final RoleRepository roleRepository;

  /**
   * 构造器注入
   *
   * @param roleRepository 角色仓储
   */
  public RoleFactory(RoleRepository roleRepository) {
    this.roleRepository = roleRepository;
  }

  /**
   * 创建角色
   *
   * @param name 角色名称
   * @param remark 备注（可空）
   * @return 角色聚合
   * @throws DomainException 角色名称已存在时抛出
   */
  public Role create(String name, @Nullable String remark) {
    // 校验角色名称唯一
    if (roleRepository.isExistByNameExcludeId(name)) {
      throw new DomainException("角色" + name + "已存在!");
    }
    return new Role(name, remark);
  }
}
