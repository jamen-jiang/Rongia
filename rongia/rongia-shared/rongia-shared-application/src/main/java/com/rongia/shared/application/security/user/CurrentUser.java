package com.rongia.shared.application.security.user;

import java.util.Set;

/** 当前登录用户 */
public interface CurrentUser {

  /**
   * 获取用户Id
   *
   * @return 用户Id
   */
  Long getUserId();

  /**
   * 获取账号
   *
   * @return 账号
   */
  String getUserName();

  /**
   * 获取用户名称
   *
   * @return 用户名称
   */
  String getName();

  /**
   * 获取手机号
   *
   * @return 手机号
   */
  String getMobilePhone();

  /**
   * 获取邮箱
   *
   * @return 邮箱
   */
  String getEmail();

  /**
   * 是否超级管理员
   *
   * @return true 表示超级管理员
   */
  boolean isSuperAdmin();

  /**
   * 获取菜单Id列表
   *
   * @return 菜单Id列表
   */
  Set<Long> getMenuIds();

  /**
   * 获取许可证列表
   *
   * @return 许可证列表
   */
  Set<String> getLicenses();

  /**
   * 获取公司Id列表
   *
   * @return 公司Id列表
   */
  Set<Long> getCompanyIds();

  /**
   * 获取部门Id列表
   *
   * @return 部门Id列表
   */
  Set<Long> getDepartmentIds();

  /**
   * 获取岗位Id列表
   *
   * @return 岗位Id列表
   */
  Set<Long> getPositionIds();
}
