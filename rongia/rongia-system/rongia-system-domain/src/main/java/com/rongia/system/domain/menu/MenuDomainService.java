package com.rongia.system.domain.menu;

import com.rongia.shared.domain.exception.DomainException;
import com.rongia.shared.domain.util.HierarchyUtils;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 菜单领域服务，负责菜单的修改和层级编排 */
@NullMarked
public class MenuDomainService {

  /** 菜单仓储 */
  private final MenuRepository menuRepository;

  /**
   * 构造器注入
   *
   * @param menuRepository 菜单仓储
   */
  public MenuDomainService(MenuRepository menuRepository) {
    this.menuRepository = menuRepository;
  }

  /**
   * 修改菜单（含层级编排）
   *
   * @param id 菜单Id
   * @param parentId 父Id（可空）
   * @param name 名称（可空）
   * @param path 路径（可空）
   * @param component 组件路径（可空）
   * @param redirect 路由重定向（可空）
   * @param license 许可证（可空）
   * @param remark 备注（可空）
   * @param title 模块标题
   * @param i18nKey 国际化Key（可空）
   * @param icon 图标（可空）
   * @param isKeepAlive 是否缓存
   * @param fixedIndexInTab 固定页签序号（可空）
   * @param isMultiTab 是否多页签
   * @param isHide 是否隐藏
   * @param sequence 排序（可空）
   * @return 需要持久化的聚合列表（自身 + 受影响的子孙）
   * @throws DomainException 菜单不存在时抛出
   */
  public List<Menu> modify(
      long id,
      @Nullable Long parentId,
      @Nullable String name,
      @Nullable String path,
      @Nullable String component,
      @Nullable String redirect,
      @Nullable String license,
      @Nullable String remark,
      String title,
      @Nullable String i18nKey,
      @Nullable String icon,
      boolean isKeepAlive,
      @Nullable Integer fixedIndexInTab,
      boolean isMultiTab,
      boolean isHide,
      @Nullable Integer sequence) {
    Menu model = menuRepository.get(id);
    if (model == null) {
      throw new DomainException("菜单不存在!");
    }
    model.modify(
        parentId,
        HierarchyUtils.resolveParentPath(parentId, getParent(parentId), "父菜单"),
        name,
        path,
        component,
        redirect,
        license,
        remark,
        title,
        i18nKey,
        icon,
        isKeepAlive,
        fixedIndexInTab,
        isMultiTab,
        isHide,
        sequence);
    List<Menu> list = new ArrayList<>();
    setChildrenParentPath(model, list);
    list.addFirst(model);
    return list;
  }

  /**
   * 删除自身及所有子孙菜单
   *
   * @param id 菜单Id
   * @return 需要持久化的聚合列表（已标记删除）
   */
  public List<Menu> removeWithChildren(long id) {
    List<Menu> list = menuRepository.getWithChildren(id);
    for (Menu item : list) {
      item.delete();
    }
    return list;
  }

  /**
   * 查询父菜单
   *
   * @param parentId 父Id（可空）
   * @return 父菜单，无父Id或不存在时返回 null（可空）
   */
  @Nullable
  private Menu getParent(@Nullable Long parentId) {
    return parentId == null ? null : menuRepository.get(parentId);
  }

  /**
   * 递归设置子菜单层级
   *
   * @param model 菜单
   * @param list 子菜单收集列表
   */
  private void setChildrenParentPath(Menu model, List<Menu> list) {
    if (model.getId() == 0) {
      return;
    }
    List<Menu> children = menuRepository.getByParentId(model.getId());
    HierarchyUtils.resetChildrenParentPath(model, children);
    for (Menu child : children) {
      setChildrenParentPath(child, list);
      list.add(child);
    }
  }
}
