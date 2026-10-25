package com.rongia.system.infrastructure.department;

import com.rongia.system.domain.department.Department;
import com.rongia.system.domain.department.DepartmentRepository;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

/** 部门仓储实现 */
@NullMarked
@Repository
public class DepartmentRepositoryImpl implements DepartmentRepository {

  /** JPA 接口 */
  private final DepartmentJpa jpa;

  /**
   * 构造器注入
   *
   * @param jpa JPA 接口
   */
  public DepartmentRepositoryImpl(DepartmentJpa jpa) {
    this.jpa = jpa;
  }

  /**
   * 按主键查询
   *
   * @param id 主键
   * @return 部门，不存在返回 null
   */
  @Override
  @Nullable
  public Department get(long id) {
    return jpa.findById(id).orElse(null);
  }

  /**
   * 查询指定部门及其所有子部门（含自身）
   *
   * @param id 部门Id
   * @return 部门及其子部门列表
   */
  @Override
  public List<Department> getWithChildren(long id) {
    return jpa.findWithChildren(id);
  }

  /**
   * 查询指定父级下的所有直接子部门
   *
   * @param parentId 父级Id
   * @return 子部门列表
   */
  @Override
  public List<Department> getListByParentId(long parentId) {
    return jpa.findByParentId(parentId);
  }

  /**
   * 判断同一父级下名称是否已存在（可排除自身）
   *
   * @param name 部门名称
   * @param parentId 父级Id（可空）
   * @param id 排除的部门Id（可空）
   * @return 是否存在
   */
  @Override
  public boolean isExistByNameAndParentIdExcludeId(
      String name, @Nullable Long parentId, @Nullable Long id) {
    return jpa.existsByNameAndParentIdExcludeId(name, parentId, id);
  }

  /**
   * 保存（新增或更新）
   *
   * @param model 部门聚合
   */
  @Override
  public void save(Department model) {
    jpa.save(model);
  }
}
