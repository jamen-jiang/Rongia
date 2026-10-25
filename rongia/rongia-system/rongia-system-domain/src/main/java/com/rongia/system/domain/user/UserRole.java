package com.rongia.system.domain.user;

import com.rongia.shared.domain.entity.BaseDeleteEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;

/** 用户绑定的角色 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "UserRole",
    indexes = {
      @Index(name = "IX_UserRole_UserId", columnList = "UserId"),
      @Index(name = "IX_UserRole_RoleId", columnList = "RoleId")
    })
public class UserRole extends BaseDeleteEntity {

  /** 用户Id */
  @Column(name = "UserId")
  private Long userId;

  /** 角色Id */
  @Column(name = "RoleId", nullable = false)
  private Long roleId;

  /**
   * 构造器
   *
   * @param roleId 角色Id
   */
  public UserRole(long roleId) {
    this.roleId = roleId;
  }
}
