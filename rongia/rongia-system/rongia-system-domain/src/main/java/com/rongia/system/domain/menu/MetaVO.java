package com.rongia.system.domain.menu;

import com.rongia.shared.domain.valueobject.ValueObject;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serial;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Nationalized;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 路由元数据 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
@Embeddable
public class MetaVO extends ValueObject {

  @Serial private static final long serialVersionUID = 1L;

  /** 模块标题 */
  @Nationalized
  @Column(name = "Title", nullable = false, length = 200)
  private String title;

  /** 国际化Key（可空） */
  @Nationalized
  @Column(name = "I18nKey", length = 200)
  @Nullable
  private String i18nKey;

  /** 图标（可空） */
  @Nationalized
  @Column(name = "Icon", length = 500)
  @Nullable
  private String icon;

  /** 是否缓存 */
  @Column(name = "IsKeepAlive", nullable = false)
  private Boolean isKeepAlive = false;

  /** 固定页签序号（可空） */
  @Column(name = "FixedIndexInTab")
  @Nullable
  private Integer fixedIndexInTab;

  /** 是否多页签 */
  @Column(name = "IsMultiTab", nullable = false)
  private Boolean isMultiTab = false;

  /** 是否隐藏 */
  @Column(name = "IsHide", nullable = false)
  private Boolean isHide = false;

  /** 排序（可空） */
  @Column(name = "Sequence")
  @Nullable
  private Integer sequence;

  /**
   * 全参构造器
   *
   * @param title 模块标题
   * @param i18nKey 国际化Key（可空）
   * @param icon 图标
   * @param isKeepAlive 是否缓存
   * @param fixedIndexInTab 固定页签序号（可空）
   * @param isMultiTab 是否多页签
   * @param isHide 是否隐藏
   * @param sequence 排序（可空）
   */
  MetaVO(
      String title,
      @Nullable String i18nKey,
      @Nullable String icon,
      boolean isKeepAlive,
      @Nullable Integer fixedIndexInTab,
      boolean isMultiTab,
      boolean isHide,
      @Nullable Integer sequence) {
    this.title = StringUtils.trimToNull(title);
    this.i18nKey = StringUtils.trimToNull(i18nKey);
    this.icon = StringUtils.trimToNull(icon);
    this.isKeepAlive = isKeepAlive;
    this.fixedIndexInTab = fixedIndexInTab;
    this.isMultiTab = isMultiTab;
    this.isHide = isHide;
    this.sequence = sequence;
  }

  /**
   * 返回参与判等的属性
   *
   * @return 属性数组
   */
  @Override
  protected @Nullable Object[] getEqualityComponents() {
    return new @Nullable Object[] {
      title, i18nKey, icon, isKeepAlive, fixedIndexInTab, isMultiTab, isHide, sequence
    };
  }
}
