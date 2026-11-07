package com.rongia.shared.infrastructure.security.user;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 登录用户快照
 *
 * <p>登录时整体存入 TokenSession（Snowy 模式），校验/取身份时直接读快照， 不再查库、不再有独立缓存层；字段可变是为了 Redis 序列化/反序列化。
 */
@Getter
@Setter
@NoArgsConstructor
public class LoginUser implements Serializable {

  /** 序列化 ID */
  @Serial private static final long serialVersionUID = 1L;

  /** 用户Id */
  private Long userId;

  /** 账号 */
  private String userName;

  /** 密码 */
  private String password;

  /** 用户名称 */
  private String name;

  /** 手机号（可空） */
  private String mobilePhone;

  /** 邮箱（可空） */
  private String email;

  /** 是否超级管理员 */
  private Boolean isSuperAdmin;

  /** 菜单Id列表，构造器中兜底为空集合 */
  private Set<Long> menuIds;

  /** 许可证列表（即权限列表），构造器中兜底为空集合 */
  private Set<String> licenses;

  /** 公司Id列表，构造器中兜底为空集合 */
  private Set<Long> companyIds;

  /** 部门Id列表，构造器中兜底为空集合 */
  private Set<Long> departmentIds;

  /** 岗位Id列表，构造器中兜底为空集合 */
  private Set<Long> positionIds;

  /**
   * 全参构造器
   *
   * @param userId 用户Id
   * @param userName 账号
   * @param password 密码
   * @param name 用户名称
   * @param mobilePhone 手机号（可空）
   * @param email 邮箱（可空）
   * @param isSuperAdmin 是否超级管理员
   * @param menuIds 菜单Id列表（可空）
   * @param licenses 许可证列表（可空）
   * @param companyIds 公司Id列表（可空）
   * @param departmentIds 部门Id列表（可空）
   * @param positionIds 岗位Id列表（可空）
   */
  public LoginUser(
      Long userId,
      String userName,
      String password,
      String name,
      String mobilePhone,
      String email,
      Boolean isSuperAdmin,
      Set<Long> menuIds,
      Set<String> licenses,
      Set<Long> companyIds,
      Set<Long> departmentIds,
      Set<Long> positionIds) {
    this.userId = userId;
    this.userName = userName;
    this.password = password;
    this.name = name;
    this.mobilePhone = mobilePhone;
    this.email = email;
    this.isSuperAdmin = isSuperAdmin;
    this.menuIds = menuIds == null ? Set.of() : menuIds;
    this.licenses = licenses == null ? Set.of() : licenses;
    this.companyIds = companyIds == null ? Set.of() : companyIds;
    this.departmentIds = departmentIds == null ? Set.of() : departmentIds;
    this.positionIds = positionIds == null ? Set.of() : positionIds;
  }

  /**
   * 账号
   *
   * @return 账号
   */
  public String getUsername() {
    return userName;
  }
}
