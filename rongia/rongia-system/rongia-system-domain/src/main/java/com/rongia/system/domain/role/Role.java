package com.rongia.system.domain.role;

import com.rongia.shared.domain.aggregate.UpdateAggregateRoot;
import com.rongia.shared.domain.exception.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Nationalized;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 角色 */
@NullMarked
@Getter
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "Role",
    indexes = {
      @Index(name = "IX_Role_Name", columnList = "Name"),
    })
public class Role extends UpdateAggregateRoot {

  /** 角色名称 */
  @Nationalized
  @Column(name = "Name", nullable = false, length = 200)
  private String name;

  /** 备注（可空） */
  @Nationalized
  @Column(name = "Remark", length = 500)
  @Nullable
  private String remark;

  /**
   * 构造器
   *
   * @param name 角色名称
   * @param remark 备注（可空）
   */
  Role(String name, @Nullable String remark) {
    String trimmedName = StringUtils.trimToNull(name);
    if (trimmedName == null) {
      throw new DomainException("角色名称不能为空");
    }
    this.name = trimmedName;
    this.remark = remark;
  }

  /**
   * 修改角色
   *
   * @param name 角色名称
   * @param remark 备注（可空）
   */
  void modify(String name, @Nullable String remark) {
    String trimmedName = StringUtils.trimToNull(name);
    if (trimmedName == null) {
      throw new DomainException("角色名称不能为空");
    }
    this.name = trimmedName;
    this.remark = remark;
  }
}
