package com.rongia.shared.infrastructure.security.permission;

import com.rongia.shared.infrastructure.security.user.LoginUser;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Set;

/** 自定义权限实现，ss 取自 Spring Security 首字母 */
@Service("ss")
public class PermissionService {

  /** 所有权限标识 */
  private static final String ALL_PERMISSION = "*:*:*";

  /**
   * 验证用户是否具备任意权限（OR 关系）
   *
   * @param permissions 权限字符串（可变参数）
   * @return true 表示拥有任意一个权限
   */
  public boolean hasPermi(String @Nullable ... permissions) {
    if (permissions == null || permissions.length == 0) {
      return false;
    }
    LoginUser loginUser = getLoginUser();
    if (loginUser == null || loginUser.getLicenses().isEmpty()) {
      return false;
    }
    // 超级管理员放行
    if (loginUser.isSuperAdmin()) {
      return true;
    }
    Set<String> authorities = loginUser.getLicenses();
    for (String permission : permissions) {
      if (hasPermissions(authorities, permission)) {
        return true;
      }
    }
    return false;
  }

  /**
   * 验证用户是否不具备某权限
   *
   * @param permission 权限字符串
   * @return 用户是否不具备某权限
   */
  public boolean lacksPermi(String permission) {
    return !hasPermi(permission);
  }

  /**
   * 判断是否包含权限
   *
   * @param permissions 权限列表
   * @param permission 权限字符串
   * @return 用户是否具备某权限
   */
  private boolean hasPermissions(Set<String> permissions, String permission) {
    return permissions.contains(ALL_PERMISSION)
        || permissions.contains(StringUtils.trim(permission));
  }

  /**
   * 获取当前登录用户
   *
   * @return 登录用户（可空），未登录返回 null
   */
  @Nullable
  private LoginUser getLoginUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
      return null;
    }
    return loginUser;
  }
}
