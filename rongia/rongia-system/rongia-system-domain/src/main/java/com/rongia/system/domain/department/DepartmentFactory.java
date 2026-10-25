package com.rongia.system.domain.department;

import com.rongia.shared.domain.exception.DomainException;
import com.rongia.shared.domain.util.HierarchyUtils;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 部门工厂，负责创建部门 */
@NullMarked
public class DepartmentFactory {

  /** 部门仓储 */
  private final DepartmentRepository departmentRepository;

  /**
   * 构造器注入
   *
   * @param departmentRepository 部门仓储
   */
  public DepartmentFactory(DepartmentRepository departmentRepository) {
    this.departmentRepository = departmentRepository;
  }

  /**
   * 创建部门
   *
   * @param companyId 公司Id
   * @param parentId 父Id（可空）
   * @param name 部门名称
   * @param disabled 是否禁用
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   * @return 部门聚合
   * @throws DomainException 同级部门名称已存在时抛出
   * @throws DomainException 父部门不存在时抛出
   */
  public Department create(
      long companyId,
      @Nullable Long parentId,
      String name,
      boolean disabled,
      @Nullable Integer sequence,
      @Nullable String remark) {
    // 校验同级部门名称唯一
    if (departmentRepository.isExistByNameAndParentIdExcludeId(name, parentId)) {
      throw new DomainException("同级部门名称" + name + "已存在!");
    }
    return new Department(
        HierarchyUtils.resolveParentPath(parentId, getParent(parentId), "父部门"),
        companyId,
        parentId,
        name,
        disabled,
        sequence,
        remark);
  }

  /**
   * 查询父部门
   *
   * @param parentId 父Id（可空）
   * @return 父部门，无父Id或不存在时返回 null（可空）
   */
  @Nullable
  private Department getParent(@Nullable Long parentId) {
    return parentId == null ? null : departmentRepository.get(parentId);
  }
}
