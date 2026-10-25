package com.rongia.system.domain.user;

import com.rongia.shared.domain.exception.DomainException;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 用户领域服务 */
@NullMarked
public class UserDomainService {

  /** 用户仓储 */
  private final UserRepository userRepository;

  /**
   * 构造器注入
   *
   * @param userRepository 用户仓储
   */
  public UserDomainService(UserRepository userRepository) {

    this.userRepository = userRepository;
  }

  /**
   * 修改用户（按Id查询后修改）
   *
   * @param id 用户Id
   * @param name 用户名称
   * @param userName 账号
   * @param email 邮箱（可空）
   * @param mobile 手机号（可空）
   * @param password 密码
   * @param disabled 是否禁用
   * @param remark 备注（可空）
   * @param roleIds 角色Id列表（可空）
   * @return 用户实体
   * @throws DomainException 用户不存在时抛出
   * @throws DomainException 名称已存在时抛出
   * @throws DomainException 账号已存在时抛出
   * @throws DomainException 邮箱已存在时抛出
   */
  public User modify(
      long id,
      String name,
      String userName,
      @Nullable String email,
      @Nullable String mobile,
      String password,
      boolean disabled,
      @Nullable String remark,
      @Nullable List<Long> roleIds) {
    // 1. 查用户
    User model = userRepository.get(id);
    if (model == null) {
      throw new DomainException("用户不存在!");
    }
    // 2. 委托给使用已有聚合的重载
    modify(model, name, userName, email, mobile, password, disabled, remark, roleIds);
    return model;
  }

  /**
   * 修改用户（使用已有聚合，避免重复查询）
   *
   * @param user 用户实体
   * @param name 用户名称
   * @param userName 账号
   * @param email 邮箱（可空）
   * @param mobile 手机号（可空）
   * @param password 密码
   * @param disabled 是否禁用
   * @param remark 备注（可空）
   * @param roleIds 角色Id列表（可空）
   * @throws DomainException 名称已存在时抛出
   * @throws DomainException 账号已存在时抛出
   * @throws DomainException 邮箱已存在时抛出
   */
  public void modify(
      User user,
      String name,
      String userName,
      @Nullable String email,
      @Nullable String mobile,
      String password,
      boolean disabled,
      @Nullable String remark,
      @Nullable List<Long> roleIds) {
    // 校验用户名称唯一（排除自身）
    if (userRepository.isExistByNameExcludeId(name, user.getId())) {
      throw new DomainException("用户名称" + name + "已存在!");
    }
    // 校验用户账号唯一（排除自身）
    if (userRepository.isExistByUserNameExcludeId(userName, user.getId())) {
      throw new DomainException("用户账号" + userName + "已存在!");
    }
    // 校验用户邮箱唯一（排除自身）
    if (StringUtils.isNotBlank(email)
        && userRepository.isExistByEmailExcludeId(email, user.getId())) {
      throw new DomainException("用户邮箱" + email + "已存在!");
    }
    user.modify(name, userName, email, mobile, password, disabled, remark, roleIds);
  }
}
