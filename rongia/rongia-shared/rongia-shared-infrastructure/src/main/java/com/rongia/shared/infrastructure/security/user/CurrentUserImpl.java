package com.rongia.shared.infrastructure.security.user;

import com.rongia.shared.application.exception.ApiException;
import com.rongia.shared.application.security.user.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;

/** 当前登录用户实现 */
@Component
public class CurrentUserImpl implements CurrentUser {

  /**
   * 获取用户Id
   *
   * @return 用户Id
   */
  @Override
  public Long getUserId() {
    return loginUser().getUserId();
  }

  /**
   * 获取账号
   *
   * @return 账号
   */
  @Override
  public String getUserName() {
    return loginUser().getUsername();
  }

  /**
   * 获取用户名称
   *
   * @return 用户名称
   */
  @Override
  public String getName() {
    return loginUser().getName();
  }

  /**
   * 获取手机号
   *
   * @return 手机号
   */
  @Override
  public String getMobilePhone() {
    return loginUser().getMobilePhone();
  }

  /**
   * 获取邮箱
   *
   * @return 邮箱
   */
  @Override
  public String getEmail() {
    return loginUser().getEmail();
  }

  /**
   * 是否超级管理员
   *
   * @return true 表示超级管理员
   */
  @Override
  public boolean isSuperAdmin() {
    return loginUser().isSuperAdmin();
  }

  /**
   * 获取应用列表
   *
   * @return 应用列表
   */
  @Override
  public Set<Long> getMenuIds() {
    return loginUser().getMenuIds();
  }

  /**
   * 获取许可证列表
   *
   * @return 许可证列表
   */
  @Override
  public Set<String> getLicenses() {
    return loginUser().getLicenses();
  }

  /**
   * 获取公司Id列表
   *
   * @return 公司Id列表
   */
  @Override
  public Set<Long> getCompanyIds() {
    return loginUser().getCompanyIds();
  }

  /**
   * 获取部门Id列表
   *
   * @return 部门Id列表
   */
  @Override
  public Set<Long> getDepartmentIds() {
    return loginUser().getDepartmentIds();
  }

  /**
   * 获取岗位Id列表
   *
   * @return 岗位Id列表
   */
  @Override
  public Set<Long> getPositionIds() {
    return loginUser().getPositionIds();
  }

  /**
   * 从 SecurityContext 取当前登录用户
   *
   * @return 登录用户
   * @throws ApiException 未登录时抛出
   */
  private LoginUser loginUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof LoginUser loginUser)) {
      throw new ApiException("未登录");
    }
    return loginUser;
  }
}
