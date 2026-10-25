package com.rongia.system.application.menu.query;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 当前用户权限响应 */
@NullMarked
@Getter
@Setter
@Schema(description = "当前用户权限响应")
public class CurrentUserPermissionResponse {

  /** 菜单列表（可空） */
  @Schema(description = "菜单列表")
  @Nullable
  private List<MenuResponse> menus;

  /** 权限列表（可空） */
  @Schema(description = "权限列表")
  @Nullable
  private List<String> licenses;
}
