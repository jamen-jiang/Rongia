package com.rongia.system.application.menu.command;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 路由元数据 */
@NullMarked
@Getter
@Schema(description = "路由元数据")
public class MetaPayload {

  /** 模块标题 */
  @Schema(description = "模块标题", requiredMode = Schema.RequiredMode.REQUIRED)
  private final String title;

  /** 国际化Key（可空） */
  @Schema(description = "国际化Key")
  @Nullable
  private final String i18nKey;

  /** 图标（可空） */
  @Schema(description = "图标")
  @Nullable
  private final String icon;

  /** 是否缓存 */
  @Schema(description = "是否缓存", requiredMode = Schema.RequiredMode.REQUIRED)
  private final Boolean isKeepAlive;

  /** 固定页签序号（可空） */
  @Schema(description = "固定页签序号")
  @Nullable
  private final Integer fixedIndexInTab;

  /** 是否多页签 */
  @Schema(description = "是否多页签", requiredMode = Schema.RequiredMode.REQUIRED)
  private final Boolean isMultiTab;

  /** 是否隐藏 */
  @Schema(description = "是否隐藏", requiredMode = Schema.RequiredMode.REQUIRED)
  private final Boolean isHide;

  /** 排序（可空） */
  @Schema(description = "排序")
  @Nullable
  private final Integer sequence;

  /**
   * 构造器
   *
   * @param title 模块标题
   * @param i18nKey 国际化Key（可空）
   * @param icon 图标（可空）
   * @param isKeepAlive 是否缓存
   * @param fixedIndexInTab 固定页签序号（可空）
   * @param isMultiTab 是否多页签
   * @param isHide 是否隐藏
   * @param sequence 排序（可空）
   */
  public MetaPayload(
      String title,
      @Nullable String i18nKey,
      @Nullable String icon,
      @Nullable Boolean isKeepAlive,
      @Nullable Integer fixedIndexInTab,
      @Nullable Boolean isMultiTab,
      @Nullable Boolean isHide,
      @Nullable Integer sequence) {
    this.title = title;
    this.i18nKey = i18nKey;
    this.icon = icon;
    this.isKeepAlive = Objects.requireNonNullElse(isKeepAlive, false);
    this.fixedIndexInTab = fixedIndexInTab;
    this.isMultiTab = Objects.requireNonNullElse(isMultiTab, false);
    this.isHide = Objects.requireNonNullElse(isHide, false);
    this.sequence = sequence;
  }
}
