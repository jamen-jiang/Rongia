package com.rongia.system.domain.user;

import com.rongia.shared.domain.exception.DomainException;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 用户工厂，负责创建用户 */
@NullMarked
public class UserFactory {

  /** 用户仓储 */
  private final UserRepository userRepository;

  /**
   * 构造器注入
   *
   * @param userRepository 用户仓储
   */
  public UserFactory(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /**
   * 创建用户
   *
   * @param name 用户名称
   * @param userName 账号
   * @param email 邮箱（可空）
   * @param mobile 手机号（可空）
   * @param password 密码
   * @param disabled 是否禁用
   * @param remark 备注（可空）
   * @param roleIds 角色Id列表（可空）
   * @return 用户实体
   * @throws DomainException 名称已存在时抛出
   * @throws DomainException 账号已存在时抛出
   * @throws DomainException 邮箱已存在时抛出
   */
  public User create(
      String name,
      String userName,
      @Nullable String email,
      @Nullable String mobile,
      String password,
      boolean disabled,
      @Nullable String remark,
      @Nullable List<Long> roleIds) {
    // 校验用户名称唯一
    if (userRepository.isExistByNameExcludeId(name)) {
      throw new DomainException("用户名称" + name + "已存在!");
    }
    // 校验用户账号唯一
    if (userRepository.isExistByUserNameExcludeId(userName)) {
      throw new DomainException("用户账号" + userName + "已存在!");
    }
    // 校验用户邮箱唯一
    if (email != null && userRepository.isExistByEmailExcludeId(email)) {
      throw new DomainException("用户邮箱" + email + "已存在!");
    }
    return new User(name, userName, email, mobile, password, disabled, remark, roleIds);
  }
}
