package com.rongia.system.infrastructure.position;

import com.rongia.system.domain.position.Position;
import com.rongia.system.domain.position.PositionRepository;
import java.util.Collection;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

/** 岗位仓储实现 */
@NullMarked
@Repository
public class PositionRepositoryImpl implements PositionRepository {

  /** JPA 接口 */
  private final PositionJpa jpa;

  /**
   * 构造器注入
   *
   * @param jpa JPA 接口
   */
  public PositionRepositoryImpl(PositionJpa jpa) {
    this.jpa = jpa;
  }

  /**
   * 按主键查询
   *
   * @param id 主键
   * @return 岗位，不存在返回 null
   */
  @Override
  @Nullable
  public Position get(long id) {
    return jpa.findById(id).orElse(null);
  }

  /**
   * 按岗位名称查询
   *
   * @param name 岗位名称
   * @return 岗位，不存在返回 null
   */
  @Override
  @Nullable
  public Position getByName(String name) {
    return jpa.findByName(name).orElse(null);
  }

  /**
   * 按部门Id列表批量查询
   *
   * @param departmentIds 部门Id列表
   * @return 岗位列表
   */
  @Override
  public List<Position> getListByDepartmentIds(Collection<Long> departmentIds) {
    if (departmentIds.isEmpty()) {
      return List.of();
    }
    return jpa.findByDepartmentIdIn(departmentIds);
  }

  /**
   * 查询指定岗位及其所有子孙岗位（含自身）
   *
   * @param id 岗位Id
   * @return 岗位及其子孙列表
   */
  @Override
  public List<Position> getWithChildren(long id) {
    return jpa.findWithChildren(id);
  }

  /**
   * 查询指定上级下的所有直接下级岗位
   *
   * @param parentId 上级岗位Id
   * @return 下级岗位列表
   */
  @Override
  public List<Position> getListByParentId(long parentId) {
    return jpa.findByParentId(parentId);
  }

  /**
   * 判断岗位名称是否已存在（可排除自身）
   *
   * @param name 岗位名称
   * @param id 排除的岗位Id（可空）
   * @return 是否存在
   */
  @Override
  public boolean isExistByNameExcludeId(String name, @Nullable Long id) {
    return jpa.existsByNameExcludeId(name, id);
  }

  /**
   * 保存（新增或更新）
   *
   * @param model 岗位聚合
   */
  @Override
  public void save(Position model) {
    jpa.save(model);
  }
}
