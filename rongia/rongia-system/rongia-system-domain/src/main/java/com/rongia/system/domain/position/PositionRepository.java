package com.rongia.system.domain.position;

import com.rongia.shared.domain.repository.Repository;
import java.util.Collection;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 岗位仓储接口 */
@NullMarked
public interface PositionRepository extends Repository<Position> {

  /**
   * 按岗位名称查询
   *
   * @param name 岗位名称
   * @return 岗位（可空），不存在返回 null
   */
  @Nullable Position getByName(String name);

  /**
   * 按部门Id列表批量查询
   *
   * @param departmentIds 部门Id列表
   * @return 岗位列表
   */
  List<Position> getListByDepartmentIds(Collection<Long> departmentIds);

  /**
   * 查询自身及所有子孙岗位（含自身）
   *
   * @param id 岗位Id
   * @return 岗位列表（含自身）
   */
  List<Position> getWithChildren(long id);

  /**
   * 按上级岗位查询直接下级
   *
   * @param parentId 上级岗位Id
   * @return 岗位列表
   */
  List<Position> getListByParentId(long parentId);

  /**
   * 判断岗位名称是否存在
   *
   * @param name 岗位名称
   * @param id 排除的岗位Id（可空，新增时传 null）
   * @return true 表示已存在
   */
  boolean isExistByNameExcludeId(String name, @Nullable Long id);

  /**
   * 判断岗位名称是否存在（新增时校验）
   *
   * @param name 岗位名称
   * @return true 表示已存在
   */
  default boolean isExistByNameExcludeId(String name) {
    return isExistByNameExcludeId(name, null);
  }
}
