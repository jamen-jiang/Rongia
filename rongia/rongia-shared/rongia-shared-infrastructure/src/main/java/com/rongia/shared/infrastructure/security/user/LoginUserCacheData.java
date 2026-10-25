package com.rongia.shared.infrastructure.security.user;

import org.jspecify.annotations.NonNull;

import java.util.Set;

/**
 * 登录用户缓存数据（不含密码）
 *
 * @param userId 用户Id
 * @param userName 账号
 * @param name 用户名称
 * @param mobilePhone 手机号（可空）
 * @param email 邮箱（可空）
 * @param isSuperAdmin 是否超级管理员
 * @param apps 菜单Id列表（可空）
 * @param licenses 许可证列表（可空）
 * @param companyIds 公司Id列表（可空）
 * @param departmentIds 部门Id列表（可空）
 * @param positionIds 岗位Id列表（可空）
 */
public record LoginUserCacheData(
    @NonNull Long userId,
    @NonNull String userName,
    @NonNull String name,
    String mobilePhone,
    String email,
    boolean isSuperAdmin,
    Set<Long> apps,
    Set<String> licenses,
    Set<Long> companyIds,
    Set<Long> departmentIds,
    Set<Long> positionIds) {

  /** 从 LoginUser 构建缓存数据（丢密码） */
  public static LoginUserCacheData from(@NonNull LoginUser u) {
    return new LoginUserCacheData(
        u.getUserId(),
        u.getUsername(),
        u.getName(),
        u.getMobilePhone(),
        u.getEmail(),
        u.isSuperAdmin(),
        u.getMenuIds(),
        u.getLicenses(),
        u.getCompanyIds(),
        u.getDepartmentIds(),
        u.getPositionIds());
  }

  /** 还原为 LoginUser（密码为 null） */
  @NonNull
  public LoginUser toLoginUser() {
    return new LoginUser(
        userId,
        userName,
        "",
        name,
        mobilePhone,
        email,
        isSuperAdmin,
        apps,
        licenses,
        companyIds,
        departmentIds,
        positionIds);
  }
}
