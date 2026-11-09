package com.rongia.platform.security;

import cn.dev33.satoken.stp.StpInterface;
import com.rongia.kernel.infrastructure.security.LoginUser;
import com.rongia.kernel.infrastructure.security.StpLoginUserUtil;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Sa-Token 权限数据源：鉴权注解（@SaCheckPermission 等）从这里取权限列表
 *
 * <p>数据来自 TokenSession 里的登录用户快照；超级管理员直接给全量通配。
 */
@Component
public class SaPermissionImpl implements StpInterface {

  /** 全量权限通配符（Sa-Token 的 vagueMatch 支持 *） */
  private static final String ALL_PERMISSION = "*";

  /**
   * 取权限列表
   *
   * @param loginId 账号Id
   * @param loginType 账号体系
   * @return 权限列表
   */
  @Override
  public List<String> getPermissionList(Object loginId, String loginType) {
    LoginUser loginUser = StpLoginUserUtil.getLoginUser();
    if (loginUser == null) {
      return List.of();
    }
    // 超级管理员放行所有权限
    if (loginUser.getIsSuperAdmin()) {
      return List.of(ALL_PERMISSION);
    }
    return List.copyOf(loginUser.getLicenses());
  }

  /**
   * 取角色列表
   *
   * @param loginId 账号Id
   * @param loginType 账号体系
   * @return 角色列表
   */
  @Override
  public List<String> getRoleList(Object loginId, String loginType) {
    return List.of();
  }
}
