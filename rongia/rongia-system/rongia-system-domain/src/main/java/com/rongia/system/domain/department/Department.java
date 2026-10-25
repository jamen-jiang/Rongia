package com.rongia.system.domain.department;

import com.rongia.shared.domain.aggregate.UpdateAggregateRoot;
import com.rongia.shared.domain.entity.Parent;
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

/**
 * 部门
 *
 * <p>字段只保留组织结构信息：Odoo 的 {@code hr.department}（name、company_id、parent_id、parent_path）、 ERPNext 的
 * {@code Department}（department_name、parent_department、company、disabled、lft/rgt）、
 * 金蝶「部门」（编码、名称、上级部门、创建组织、部门属性）都只有这些——**没有一家在部门上存地址或电话**， 联系信息属于公司或员工/岗位。
 *
 * <p>负责人（Odoo {@code manager_id}）与成员（{@code member_ids}）等员工主数据建立后再加。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "Department",
    indexes = {
      @Index(name = "IX_Department_CompanyId", columnList = "CompanyId"),
      @Index(name = "IX_Department_ParentId", columnList = "ParentId"),
      @Index(name = "IX_Department_ParentPath", columnList = "ParentPath"),
      @Index(name = "IX_Department_Name", columnList = "Name"),
      @Index(name = "IX_Department_Sequence", columnList = "Sequence")
    })
public class Department extends UpdateAggregateRoot implements Parent {

  /** 公司Id */
  @Column(name = "CompanyId", nullable = false)
  private Long companyId;

  /** 父Id（可空） */
  @Column(name = "ParentId")
  @Nullable
  private Long parentId;

  /** 父级路径（可空，形如 "1-3-5"） */
  @Nationalized
  @Column(name = "ParentPath")
  @Nullable
  private String parentPath;

  /** 名称 */
  @Nationalized
  @Column(name = "Name", nullable = false, length = 200)
  private String name;

  /** 排序（可空） */
  @Column(name = "Sequence")
  @Nullable
  private Integer sequence;

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
   * @param parentPath 父级路径（可空）
   * @param companyId 公司Id
   * @param parentId 父Id（可空）
   * @param name 名称
   * @param disabled 是否禁用
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   */
  public Department(
      @Nullable String parentPath,
      long companyId,
      @Nullable Long parentId,
      String name,
      boolean disabled,
      @Nullable Integer sequence,
      @Nullable String remark) {
    apply(parentPath, companyId, parentId, name, disabled, sequence, remark);
  }

  /**
   * 修改部门
   *
   * @param parentPath 父级路径（可空）
   * @param companyId 公司Id
   * @param parentId 父Id（可空）
   * @param name 名称
   * @param disabled 是否禁用
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   */
  public void modify(
      @Nullable String parentPath,
      long companyId,
      @Nullable Long parentId,
      String name,
      boolean disabled,
      @Nullable Integer sequence,
      @Nullable String remark) {
    apply(parentPath, companyId, parentId, name, disabled, sequence, remark);
  }

  /**
   * 设置层级
   *
   * @param parentPath 父级路径
   */
  @Override
  public void changeParentPath(@Nullable String parentPath) {
    this.parentPath = parentPath;
  }

  // ==================== 私有：校验与赋值 ====================

  /**
   * 统一的校验与赋值入口（构造器与修改共用）
   *
   * @param parentPath 父级路径（可空）
   * @param companyId 公司Id
   * @param parentId 父Id（可空）
   * @param name 名称
   * @param disabled 是否禁用
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   * @throws DomainException 名称为空时抛出
   */
  private void apply(
      @Nullable String parentPath,
      long companyId,
      @Nullable Long parentId,
      String name,
      boolean disabled,
      @Nullable Integer sequence,
      @Nullable String remark) {
    String trimmedName = StringUtils.trimToNull(name);
    if (trimmedName == null) {
      throw new DomainException("部门名称不能为空");
    }
    this.parentPath = StringUtils.trimToNull(parentPath);
    this.companyId = companyId;
    this.parentId = parentId;
    this.name = trimmedName;
    this.disabled = disabled;
    this.sequence = sequence;
    this.remark = remark;
  }
}
