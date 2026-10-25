package com.rongia.system.domain.menu;

import com.rongia.shared.domain.repository.Repository;
import java.util.List;
import org.jspecify.annotations.NullMarked;

/** 菜单仓储接口 */
@NullMarked
public interface MenuRepository extends Repository<Menu> {

  /**
   * 查询指定菜单及其所有子菜单（包含自身）
   *
   * @param id 菜单ID
   * @return 菜单及其子菜单列表
   */
  List<Menu> getWithChildren(long id);

  /**
   * 查询指定父级下的所有直接子菜单
   *
   * @param parentId 父级ID
   * @return 子菜单实体列表
   */
  List<Menu> getByParentId(long parentId);
}
