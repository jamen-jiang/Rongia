package com.rongia.system.domain.menu;

import com.rongia.shared.domain.aggregate.UpdateAggregateRoot;
import com.rongia.shared.domain.entity.Parent;
import com.rongia.shared.domain.exception.DomainException;
import com.rongia.shared.domain.util.EnumUtils;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Nationalized;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 菜单 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "Menu",
    indexes = {
      @Index(name = "IX_Menu_Type", columnList = "Type"),
      @Index(name = "IX_Menu_ParentId", columnList = "ParentId"),
      @Index(name = "IX_Menu_ParentPath", columnList = "ParentPath"),
      @Index(name = "IX_Menu_Name", columnList = "Name"),
      @Index(name = "IX_Menu_Sequence", columnList = "Sequence"),
    })
public class Menu extends UpdateAggregateRoot implements Parent {

  /** 菜单类型编码，取值见 {@link MenuTypeEnum} */
  @Nationalized
  @Column(name = "Type", nullable = false, length = 50)
  private String type;

  /** 父Id（可空） */
  @Column(name = "ParentId")
  @Nullable
  private Long parentId;

  /** 父级路径（可空，形如 "1-3-5"） */
  @Nationalized
  @Column(name = "ParentPath")
  @Nullable
  private String parentPath;

  /** 名称（可空） */
  @Nationalized
  @Column(name = "Name", length = 200)
  @Nullable
  private String name;

  /** 路径（可空） */
  @Nationalized
  @Column(name = "Path", length = 500)
  @Nullable
  private String path;

  /** 组件路径（可空） */
  @Nationalized
  @Column(name = "Component", length = 500)
  @Nullable
  private String component;

  /** 路由重定向（可空） */
  @Nationalized
  @Column(name = "Redirect", length = 500)
  @Nullable
  private String redirect;

  /** 许可证（可空） */
  @Nationalized
  @Column(name = "License", length = 200)
  @Nullable
  private String license;

  /** 备注（可空） */
  @Nationalized
  @Column(name = "Remark", length = 500)
  @Nullable
  private String remark;

  /** 路由元数据 */
  @Embedded private MetaVO meta;

  Menu(
      String type,
      @Nullable Long parentId,
      @Nullable String parentPath,
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
    this.type = EnumUtils.getCode(MenuTypeEnum.class, type, "菜单的类型");
    this.parentId = parentId;
    this.parentPath = StringUtils.trimToNull(parentPath);
    this.name = StringUtils.trimToNull(name);
    this.path = StringUtils.trimToNull(path);
    this.component = StringUtils.trimToNull(component);
    this.redirect = StringUtils.trimToNull(redirect);
    this.license = StringUtils.trimToNull(license);
    this.remark = remark;
    this.meta =
        new MetaVO(
            title, i18nKey, icon, isKeepAlive, fixedIndexInTab, isMultiTab, isHide, sequence);
  }

  void modify(
      @Nullable Long parentId,
      @Nullable String parentPath,
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
    if (parentId != null && parentId.equals(this.getId())) {
      throw new DomainException("上级菜单不能是自己!");
    }
    this.parentId = parentId;
    this.parentPath = parentPath;
    this.name = StringUtils.trimToNull(name);
    this.path = StringUtils.trimToNull(path);
    this.component = StringUtils.trimToNull(component);
    this.redirect = StringUtils.trimToNull(redirect);
    this.license = StringUtils.trimToNull(license);
    this.remark = remark;
    this.meta =
        new MetaVO(
            title, i18nKey, icon, isKeepAlive, fixedIndexInTab, isMultiTab, isHide, sequence);
  }

  @Override
  public void changeParentPath(@Nullable String parentPath) {
    this.parentPath = parentPath;
  }
}
