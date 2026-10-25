package com.rongia.system.infrastructure.menu;

import com.rongia.system.domain.menu.Menu;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/** 菜单 JPA 接口 */
@NullMarked
@Repository
public interface MenuJpa extends JpaRepository<Menu, Long>, JpaSpecificationExecutor<Menu> {

  /**
   * 按Id集合查菜单
   *
   * @param ids 菜单Id集合
   * @return 菜单集合
   */
  Set<Menu> findByIdIn(Set<Long> ids);

  /**
   * 查询指定父级下的所有直接子菜单
   *
   * @param parentId 父菜单Id
   * @return 子菜单列表
   */
  List<Menu> findByParentId(long parentId);

  /**
   * 查询自身及所有后代
   *
   * @param id 菜单 ID
   * @return 菜单及其后代列表
   */
  default List<Menu> findWithChildren(long id) {
    Specification<Menu> spec =
        (root, query, cb) ->
            cb.or(
                cb.equal(root.get("id"), id),
                cb.like(cb.concat(cb.concat("-", root.get("parentPath")), "-"), "%-" + id + "-%"));
    return findAll(spec);
  }
}
