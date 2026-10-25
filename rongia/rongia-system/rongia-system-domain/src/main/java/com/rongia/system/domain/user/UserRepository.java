package com.rongia.system.domain.user;

import com.rongia.shared.domain.repository.Repository;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 用户仓储接口 */
@NullMarked
public interface UserRepository extends Repository<User> {

  /**
   * 按账号查询用户
   *
   * @param userName 账号
   * @return 用户实体（可空），不存在返回 null
   */
  @Nullable User getByUserName(String userName);

  /**
   * 判断名称是否存在
   *
   * @param name 用户名称
   * @param id 排除的用户Id（可空，更新时排除自身）
   * @return true 表示已存在
   */
  boolean isExistByNameExcludeId(String name, @Nullable Long id);

  /**
   * 判断名称是否存在（新增场景）
   *
   * @param name 用户名称
   * @return true 表示已存在
   */
  default boolean isExistByNameExcludeId(String name) {
    return isExistByNameExcludeId(name, null);
  }

  /**
   * 判断账号是否存在
   *
   * @param userName 账号
   * @param id 排除的用户Id（可空，更新时排除自身）
   * @return true 表示已存在
   */
  boolean isExistByUserNameExcludeId(String userName, @Nullable Long id);

  /**
   * 判断账号是否存在（新增场景）
   *
   * @param userName 账号
   * @return true 表示已存在
   */
  default boolean isExistByUserNameExcludeId(String userName) {
    return isExistByUserNameExcludeId(userName, null);
  }

  /**
   * 判断邮箱是否存在
   *
   * @param email 邮箱（可空）
   * @param id 排除的用户Id（可空，更新时排除自身）
   * @return true 表示已存在
   */
  boolean isExistByEmailExcludeId(String email, @Nullable Long id);

  /**
   * 判断邮箱是否存在（新增场景）
   *
   * @param email 邮箱（可空）
   * @return true 表示已存在
   */
  default boolean isExistByEmailExcludeId(String email) {
    return isExistByEmailExcludeId(email, null);
  }
}
