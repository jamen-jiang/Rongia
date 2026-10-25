package com.rongia.system.infrastructure.security.user;

import com.rongia.shared.infrastructure.security.user.LoginUser;
import com.rongia.system.domain.user.User;
import com.rongia.system.infrastructure.user.UserJpa;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 用户详情服务：把 User 聚合装配成 Spring Security 用的 LoginUser */
@Service
public class LoginUserService implements UserDetailsService {

  /** 用户 JPA */
  private final UserJpa userJpa;

  /** 用户权限加载器 */
  private final UserPermissionLoader permissionLoader;

  /**
   * 构造器注入
   *
   * @param userJpa 用户 JPA
   * @param permissionLoader 用户权限加载器
   */
  public LoginUserService(UserJpa userJpa, UserPermissionLoader permissionLoader) {
    this.userJpa = userJpa;
    this.permissionLoader = permissionLoader;
  }

  /**
   * 按账号加载用户
   *
   * @param userName 账号
   * @return UserDetails
   * @throws UsernameNotFoundException 用户不存在时抛出
   */
  @Override
  @Transactional(readOnly = true)
  public @NonNull UserDetails loadUserByUsername(@NonNull String userName)
      throws UsernameNotFoundException {
    User model =
        userJpa
            .findByUserName(userName)
            .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + userName));
    return buildLoginUser(model);
  }

  /**
   * 按用户Id加载用户
   *
   * @param userId 用户Id
   * @return LoginUser
   * @throws UsernameNotFoundException 用户不存在时抛出
   */
  @Transactional(readOnly = true)
  public LoginUser loadUserByUserId(@NonNull Long userId) throws UsernameNotFoundException {
    User model =
        userJpa
            .findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + userId));
    return buildLoginUser(model);
  }

  /**
   * 构建 LoginUser（含数据范围与许可）
   *
   * @param model 用户聚合
   * @return LoginUser
   */
  private LoginUser buildLoginUser(User model) {
    UserPermissionLoader.UserPermissionData permission = permissionLoader.load(model);
    return new LoginUser(
        model.getId(),
        model.getUserName(),
        model.getPassword(),
        model.getName(),
        null,
        model.getEmail(),
        permission.superAdmin,
        permission.menuIds,
        permission.licenses,
        permission.companyIds,
        permission.departmentIds,
        permission.positionIds);
  }
}
