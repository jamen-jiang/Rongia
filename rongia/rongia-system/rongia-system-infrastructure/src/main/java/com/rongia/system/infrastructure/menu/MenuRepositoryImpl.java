package com.rongia.system.infrastructure.menu;

import com.rongia.system.domain.menu.Menu;
import com.rongia.system.domain.menu.MenuRepository;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

/** 菜单仓储实现 */
@NullMarked
@Repository
public class MenuRepositoryImpl implements MenuRepository {

  /** JPA 接口 */
  private final MenuJpa jpa;

  /**
   * 构造器注入
   *
   * @param jpa JPA 接口
   */
  public MenuRepositoryImpl(MenuJpa jpa) {
    this.jpa = jpa;
  }

  /**
   * 按主键查询
   *
   * @param id 主键
   * @return 菜单，不存在返回 null
   */
  @Override
  @Nullable
  public Menu get(long id) {
    return jpa.findById(id).orElse(null);
  }

  /**
   * 查询指定菜单及其所有子菜单（含自身）
   *
   * @param id 菜单Id
   * @return 菜单及子菜单列表
   */
  @Override
  public List<Menu> getWithChildren(long id) {
    return jpa.findWithChildren(id);
  }

  /**
   * 查询指定父级下的所有直接子菜单
   *
   * @param parentId 父菜单Id
   * @return 子菜单列表
   */
  @Override
  public List<Menu> getByParentId(long parentId) {
    return jpa.findByParentId(parentId);
  }

  /**
   * 保存（新增或更新）
   *
   * @param model 菜单聚合
   */
  @Override
  public void save(Menu model) {
    jpa.save(model);
  }
}
