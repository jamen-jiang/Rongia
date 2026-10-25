package com.rongia.system.domain.user;

import com.rongia.shared.domain.aggregate.UpdateAggregateRoot;
import com.rongia.shared.domain.exception.DomainException;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.SQLRestriction;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 用户（登录身份）
 *
 * <p>字段对照别家：Odoo {@code res.users}（name、login、password、active、groups_id）、 Frappe/ERPNext {@code
 * User}（email 即主键、first_name、enabled、roles 子表）——两家都用 **邮箱或登录名**标识身份、都有启用开关、角色都挂在用户上。我们比他们多一层「岗位 →
 * 角色」， 因为还没有员工主数据（两家的岗位/职务挂在 Employee 上，不在用户上）。
 *
 * <p>不抄的：Odoo 的个人偏好（语言/时区/签名）与公司归属、Frappe 的通讯录字段 （gender/birth_date/location）、last_login 与
 * api_key——都要等真有消费它们的流程。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "User",
    indexes = {
      @Index(name = "IX_User_Name", columnList = "Name"),
      @Index(name = "IX_User_UserName", columnList = "UserName")
      // 注意：UserName 的过滤唯一索引带 WHERE IsDeleted=0，JPA 不支持，用 Flyway 创建
    })
public class User extends UpdateAggregateRoot {

  /** 用户名称 */
  @Nationalized
  @Column(name = "Name", nullable = false, length = 200)
  private String name;

  /** 账号 */
  @Nationalized
  @Column(name = "UserName", nullable = false, length = 50)
  private String userName;

  /** 手机号（可空） */
  @Nationalized
  @Column(name = "Mobile", length = 50)
  @Nullable
  private String mobile;

  /** 邮箱（可空） */
  @Nationalized
  @Column(name = "Email", length = 200)
  @Nullable
  private String email;

  /** 密码 */
  @Nationalized
  @Column(name = "Password", nullable = false, length = 50)
  private String password;

  /** 是否禁用 */
  @Column(name = "Disabled", nullable = false)
  private Boolean disabled = false;

  /** 备注（可空） */
  @Nationalized
  @Column(name = "Remark", length = 500)
  @Nullable
  private String remark;

  /** 用户角色列表 */
  @OneToMany(
      fetch = FetchType.EAGER,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE},
      orphanRemoval = true)
  @JoinColumn(name = "UserId")
  @SQLRestriction("IsDeleted = 0")
  private List<UserRole> userRoles = new ArrayList<>();

  /**
   * 获取用户角色列表（只读）
   *
   * @return 不可变视图
   */
  public List<UserRole> getUserRoles() {
    return Collections.unmodifiableList(userRoles);
  }

  /**
   * 构造器
   *
   * @param name 用户名称
   * @param userName 账号
   * @param email 邮箱（可空）
   * @param mobile 手机号（可空）
   * @param password 密码
   * @param disabled 是否禁用
   * @param remark 备注（可空）
   * @param roleIds 角色Id列表（可空）
   */
  User(
      String name,
      String userName,
      @Nullable String email,
      @Nullable String mobile,
      String password,
      boolean disabled,
      @Nullable String remark,
      @Nullable List<Long> roleIds) {
    apply(name, userName, email, mobile, password, disabled, remark, roleIds);
  }

  /**
   * 修改用户
   *
   * @param name 用户名称
   * @param userName 账号
   * @param email 邮箱（可空）
   * @param mobile 手机号（可空）
   * @param password 密码
   * @param disabled 是否禁用
   * @param remark 备注（可空）
   * @param roleIds 角色Id列表（可空）
   */
  void modify(
      String name,
      String userName,
      @Nullable String email,
      @Nullable String mobile,
      String password,
      boolean disabled,
      @Nullable String remark,
      @Nullable List<Long> roleIds) {
    apply(name, userName, email, mobile, password, disabled, remark, roleIds);
  }

  // ==================== 私有：校验与赋值 ====================

  /**
   * 统一的校验与赋值入口（构造器与修改共用）
   *
   * @param name 用户名称
   * @param userName 账号
   * @param email 邮箱（可空）
   * @param mobile 手机号（可空）
   * @param password 密码
   * @param disabled 是否禁用
   * @param remark 备注（可空）
   * @param roleIds 角色Id列表（可空）
   * @throws DomainException 名称/账号/密码为空、或岗位为空时抛出
   */
  private void apply(
      String name,
      String userName,
      @Nullable String email,
      @Nullable String mobile,
      String password,
      boolean disabled,
      @Nullable String remark,
      @Nullable List<Long> roleIds) {
    String trimmedName = StringUtils.trimToNull(name);
    String trimmedUserName = StringUtils.trimToNull(userName);
    if (trimmedName == null) {
      throw new DomainException("用户名称不能为空");
    }
    if (trimmedUserName == null) {
      throw new DomainException("账号不能为空");
    }
    if (StringUtils.isBlank(password)) {
      throw new DomainException("密码不能为空");
    }
    this.name = trimmedName;
    this.userName = trimmedUserName;
    this.email = StringUtils.trimToNull(email);
    this.mobile = StringUtils.trimToNull(mobile);
    this.password = password;
    this.disabled = disabled;
    this.remark = remark;
    syncRoles(roleIds);
  }

  /**
   * 同步角色列表
   *
   * @param roleIds 角色Id列表（可空）
   */
  private void syncRoles(@Nullable List<Long> roleIds) {
    List<Long> target = roleIds == null ? List.of() : roleIds;

    // 1. 删除未在输入中出现的角色
    List<UserRole> toRemoves =
        userRoles.stream()
            .filter(a -> target.stream().noneMatch(i -> Objects.equals(i, a.getRoleId())))
            .toList();
    for (UserRole item : toRemoves) {
      deleteRole(item.getId());
    }

    // 2. 添加输入中不存在的角色
    for (Long roleId : target) {
      boolean exists =
          userRoles.stream()
              .anyMatch(x -> Objects.equals(x.getRoleId(), roleId) && !x.getIsDeleted());
      if (!exists) {
        userRoles.add(new UserRole(roleId));
      }
    }
  }

  /**
   * 删除角色
   *
   * @param userRoleId 用户角色关联Id
   */
  private void deleteRole(long userRoleId) {
    userRoles.stream()
        .filter(x -> Objects.equals(x.getId(), userRoleId))
        .findFirst()
        .ifPresent(UserRole::delete);
  }
}
