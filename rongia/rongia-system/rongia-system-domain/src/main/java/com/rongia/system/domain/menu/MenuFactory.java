package com.rongia.system.domain.menu;

import com.rongia.shared.domain.exception.DomainException;
import com.rongia.shared.domain.util.HierarchyUtils;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 菜单工厂，负责创建菜单 */
@NullMarked
public class MenuFactory {

  /** 菜单仓储 */
  private final MenuRepository menuRepository;

  /**
   * 构造器注入
   *
   * @param menuRepository 菜单仓储
   */
  public MenuFactory(MenuRepository menuRepository) {
    this.menuRepository = menuRepository;
  }

  /**
   * 创建菜单
   *
   * @param type 类型
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
   * @return 菜单聚合
   * @throws DomainException 父菜单不存在时抛出
   */
  public Menu create(
      String type,
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
    return new Menu(
        type,
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
}
