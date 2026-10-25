package com.rongia.system.infrastructure.user;

import com.rongia.system.domain.user.User;
import com.rongia.system.domain.user.UserRepository;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

/** 用户仓储实现 */
@NullMarked
@Repository
public class UserRepositoryImpl implements UserRepository {

  /** JPA 接口 */
  private final UserJpa jpa;

  /**
   * 构造器注入
   *
   * @param jpa JPA 接口
   */
  public UserRepositoryImpl(UserJpa jpa) {
    this.jpa = jpa;
  }

  /**
   * 按主键查询
   *
   * @param id 主键
   * @return 用户，不存在返回 null
   */
  @Override
  @Nullable
  public User get(long id) {
    return jpa.findById(id).orElse(null);
  }

  /**
   * 按账号查询用户
   *
   * @param userName 账号
   * @return 用户，不存在返回 null
   */
  @Override
  @Nullable
  public User getByUserName(String userName) {
    return jpa.findByUserName(userName).orElse(null);
  }

  /**
   * 判断用户名称是否已存在（可排除自身）
   *
   * @param name 用户名称
   * @param id 排除的用户Id（可空）
   * @return 是否存在
   */
  @Override
  public boolean isExistByNameExcludeId(String name, @Nullable Long id) {
    return jpa.existsByNameExcludeId(name, id);
  }

  /**
   * 判断账号是否已存在（可排除自身）
   *
   * @param userName 账号
   * @param id 排除的用户Id（可空）
   * @return 是否存在
   */
  @Override
  public boolean isExistByUserNameExcludeId(String userName, @Nullable Long id) {
    return jpa.existsByUserNameExcludeId(userName, id);
  }

  /**
   * 判断邮箱是否已存在（可排除自身）
   *
   * @param email 邮箱（可空）
   * @param id 排除的用户Id（可空）
   * @return 是否存在
   */
  @Override
  public boolean isExistByEmailExcludeId(String email, @Nullable Long id) {
    return jpa.existsByEmailExcludeId(email, id);
  }

  /**
   * 保存（新增或更新）
   *
   * @param model 用户聚合
   */
  @Override
  public void save(User model) {
    jpa.save(model);
  }
}
