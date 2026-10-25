package com.rongia.system.application.menu.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 路由元数据响应
 *
 * @param title 模块标题
 * @param i18nKey 国际化Key（可空）
 * @param icon 图标（可空）
 * @param isKeepAlive 是否缓存
 * @param fixedIndexInTab 固定在页签中的序号（可空）
 * @param isMultiTab 是否支持多个tab页签
 * @param isHide 是否隐藏
 * @param sequence 排序（可空）
 */
@NullMarked
@Schema(description = "路由元数据响应")
public record MetaResponse(
    @Schema(description = "模块标题", requiredMode = Schema.RequiredMode.REQUIRED) String title,
    @Schema(description = "国际化Key") @Nullable String i18nKey,
    @Schema(description = "图标") @Nullable String icon,
    @Schema(description = "是否缓存", requiredMode = Schema.RequiredMode.REQUIRED) Boolean isKeepAlive,
    @Schema(description = "固定页签序号") @Nullable Integer fixedIndexInTab,
    @Schema(description = "是否多页签", requiredMode = Schema.RequiredMode.REQUIRED) Boolean isMultiTab,
    @Schema(description = "是否隐藏", requiredMode = Schema.RequiredMode.REQUIRED) Boolean isHide,
    @Schema(description = "排序") @Nullable Integer sequence) {

  /** 是否缓存（别名） */
  @JsonProperty("keepAlive")
  public boolean keepAlive() {
    return isKeepAlive;
  }

  /** 是否支持多个tab页签（别名） */
  @JsonProperty("multiTab")
  public boolean multiTab() {
    return isMultiTab;
  }

  /** 是否隐藏在菜单（别名） */
  @JsonProperty("hideInMenu")
  public boolean hideInMenu() {
    return isHide;
  }

  /** 排序（别名） */
  @JsonProperty("order")
  @Nullable
  public Integer order() {
    return sequence;
  }
}
