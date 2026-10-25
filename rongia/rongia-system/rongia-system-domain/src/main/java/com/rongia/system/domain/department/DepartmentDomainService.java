package com.rongia.system.domain.department;

import com.rongia.shared.domain.exception.DomainException;
import com.rongia.shared.domain.util.HierarchyUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 部门领域服务，负责层级编排 */
@NullMarked
public class DepartmentDomainService {

  /** 部门仓储 */
  private final DepartmentRepository departmentRepository;

  /**
   * 构造器注入
   *
   * @param departmentRepository 部门仓储
   */
  public DepartmentDomainService(DepartmentRepository departmentRepository) {
    this.departmentRepository = departmentRepository;
  }

  /**
   * 修改部门（含层级编排）
   *
   * @param id 部门Id
   * @param companyId 公司Id
   * @param parentId 父Id（可空）
   * @param name 部门名称
   * @param disabled 是否禁用
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   * @return 需要持久化的聚合列表（自身 + 受影响的子孙）
   * @throws DomainException 同级部门名称已存在时抛出
   * @throws DomainException 部门不存在时抛出
   * @throws DomainException 父部门与部门不同公司、或父部门是自己/自己的子孙时抛出
   */
  public List<Department> modify(
      long id,
      long companyId,
      @Nullable Long parentId,
      String name,
      boolean disabled,
      @Nullable Integer sequence,
      @Nullable String remark) {
    // 校验同级部门名称唯一（排除自身）
    if (departmentRepository.isExistByNameAndParentIdExcludeId(name, parentId, id)) {
      throw new DomainException("同级部门名称" + name + "已存在!");
    }
    Department model = departmentRepository.get(id);
    if (model == null) {
      throw new DomainException("部门不存在!");
    }
    Department parent = getParent(parentId);
    validateParent(model, companyId, parentId, parent);
    model.modify(
        HierarchyUtils.resolveParentPath(parentId, parent, "父部门"),
        companyId,
        parentId,
        name,
        disabled,
        sequence,
        remark);
    List<Department> list = new ArrayList<>();
    setChildrenParentPath(model, list);
    list.addFirst(model);
    return list;
  }

  /**
   * 删除自身及所有子孙部门
   *
   * @param id 部门Id
   * @return 需要持久化的聚合列表（已标记删除）
   */
  public List<Department> removeWithChildren(long id) {
    List<Department> list = departmentRepository.getWithChildren(id);
    for (Department item : list) {
      item.delete();
    }
    return list;
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

  /**
   * 校验父部门合法
   *
   * <p>三条规矩都跟别家一致（Odoo 的 {@code parent_id} 带 {@code check_company=True}、ERPNext 的部门树也
   * 不跨公司）：父部门必须跟本部门在同一个公司下、不能是自己、不能是自己的子孙——第三条不拦的话，
   * 层级路径会绕成一个环，"整棵子树"的查询和删除都会跟着出错。
   *
   * @param model 部门
   * @param companyId 本部门所属公司Id
   * @param parentId 父部门Id（可空）
   * @param parent 父部门（可空）
   * @throws DomainException 父部门与部门不同公司、或父部门是自己/自己的子孙时抛出
   */
  private void validateParent(
      Department model, long companyId, @Nullable Long parentId, @Nullable Department parent) {
    if (parentId == null) {
      return;
    }
    if (Objects.equals(parentId, model.getId())) {
      throw new DomainException("父部门不能是自己!");
    }
    if (parent != null && !Objects.equals(parent.getCompanyId(), companyId)) {
      throw new DomainException("父部门必须在同一公司下!");
    }
    boolean isDescendant =
        departmentRepository.getWithChildren(model.getId()).stream()
            .anyMatch(item -> Objects.equals(item.getId(), parentId));
    if (isDescendant) {
      throw new DomainException("父部门不能是自己的子孙部门!");
    }
  }

  /**
   * 递归设置子部门层级
   *
   * @param model 部门
   * @param list 子部门收集列表
   */
  private void setChildrenParentPath(Department model, List<Department> list) {
    if (model.getId() == 0) {
      return;
    }
    List<Department> children = departmentRepository.getListByParentId(model.getId());
    HierarchyUtils.resetChildrenParentPath(model, children);
    for (Department child : children) {
      setChildrenParentPath(child, list);
      list.add(child);
    }
  }
}
