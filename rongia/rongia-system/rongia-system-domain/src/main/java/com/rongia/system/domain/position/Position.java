package com.rongia.system.domain.position;

import com.rongia.shared.domain.aggregate.UpdateAggregateRoot;
import com.rongia.shared.domain.entity.Parent;
import com.rongia.shared.domain.exception.DomainException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Nationalized;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 岗位
 *
 * <p>上级岗位构成一棵树（金蝶的岗位就带「上级岗位」：业务员的上级是业务组长），层级路径由 {@link PositionDomainService} 维护，与 {@code
 * Category}、{@code Department}、{@code Company} 同一套。
 *
 * <p>岗位不携带角色。授权只有两张写法：**角色只挂人**（{@code UserRole}）， **谁能用哪个应用/资源走 {@code Privilege}**（主体可以是 Role /
 * User / Department / Position）。 所以给岗位授权就是往 {@code Privilege} 写一行 {@code Master='Position',
 * MasterValue=岗位Id}， 原来的 {@code PostRole} 中转表已删——同一件事不需要两套实现。
 *
 * <p>不挂职级：Odoo {@code hr.job} 没有级别字段、ERPNext 的级别（{@code Employee Grade}）挂在员工上、 SAP
 * 的薪酬等级也属人事薪酬——「同一岗位不同人不同级」是现实，职级等员工主数据建立后再决定挂哪里。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "Position",
    indexes = {
      @Index(name = "IX_Post_DepartmentId", columnList = "DepartmentId"),
      @Index(name = "IX_Post_ParentId", columnList = "ParentId"),
      @Index(name = "IX_Post_ParentPath", columnList = "ParentPath"),
      @Index(name = "IX_Post_Name", columnList = "Name"),
    })
public class Position extends UpdateAggregateRoot implements Parent {

  /** 部门Id */
  @Column(name = "DepartmentId", nullable = false)
  private Long departmentId;

  /** 上级岗位Id（可空，空表示顶级） */
  @Column(name = "ParentId")
  @Nullable
  private Long parentId;

  /** 层级路径（可空，形如 "1-3-5"，由层级编排逻辑维护） */
  @Nationalized
  @Column(name = "ParentPath")
  @Nullable
  private String parentPath;

  /** 岗位名称 */
  @Nationalized
  @Column(name = "Name", nullable = false, length = 200)
  private String name;

  /** 是否禁用 */
  @Column(name = "Disabled", nullable = false)
  private Boolean disabled = false;

  /** 备注（可空） */
  @Nationalized
  @Column(name = "Remark", length = 500)
  @Nullable
  private String remark;

  /**
   * 构造器
   *
   * @param name 岗位名称
   * @param departmentId 部门Id
   * @param parentId 上级岗位Id（可空）
   * @param parentPath 层级路径（可空）
   * @param disabled 是否禁用
   * @param remark 备注（可空）
   */
  Position(
      String name,
      long departmentId,
      @Nullable Long parentId,
      @Nullable String parentPath,
      boolean disabled,
      @Nullable String remark) {
    apply(name, departmentId, parentId, parentPath, disabled, remark);
  }

  /**
   * 修改岗位
   *
   * @param name 岗位名称
   * @param departmentId 部门Id
   * @param parentId 上级岗位Id（可空）
   * @param parentPath 层级路径（可空）
   * @param disabled 是否禁用
   * @param remark 备注（可空）
   */
  void modify(
      String name,
      long departmentId,
      @Nullable Long parentId,
      @Nullable String parentPath,
      boolean disabled,
      @Nullable String remark) {
    apply(name, departmentId, parentId, parentPath, disabled, remark);
  }

  /**
   * 设置层级
   *
   * @param parentPath 父级路径（可空）
   */
  @Override
  public void changeParentPath(@Nullable String parentPath) {
    this.parentPath = parentPath;
  }

  // ==================== 私有：校验与赋值 ====================

  /**
   * 统一的校验与赋值入口（构造器与修改共用）
   *
   * @param name 岗位名称
   * @param departmentId 部门Id
   * @param parentId 上级岗位Id（可空）
   * @param parentPath 层级路径（可空）
   * @param disabled 是否禁用
   * @param remark 备注（可空）
   * @throws DomainException 岗位名称为空时抛出
   */
  private void apply(
      String name,
      long departmentId,
      @Nullable Long parentId,
      @Nullable String parentPath,
      boolean disabled,
      @Nullable String remark) {
    String trimmedName = StringUtils.trimToNull(name);
    if (trimmedName == null) {
      throw new DomainException("岗位名称不能为空");
    }
    this.name = trimmedName;
    this.departmentId = departmentId;
    this.parentId = parentId;
    this.parentPath = StringUtils.trimToNull(parentPath);
    this.disabled = disabled;
    this.remark = remark;
  }
}
