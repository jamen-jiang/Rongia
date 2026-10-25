package com.rongia.system.application.menu.query;

import com.rongia.shared.application.response.BaseResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 菜单响应 */
@NullMarked
@Getter
@Setter
@Schema(description = "菜单响应")
public class MenuResponse extends BaseResponse {

  /** 菜单Id */
  @Schema(description = "菜单Id", requiredMode = Schema.RequiredMode.REQUIRED)
  private Long id;

  /** 类型 */
  @Schema(description = "类型", requiredMode = Schema.RequiredMode.REQUIRED)
  private String type;

  /** 父Id（可空） */
  @Schema(description = "父Id")
  @Nullable
  private Long parentId;

  /** 父级路径（可空，形如 "1-3-5"） */
  @Schema(description = "父级路径")
  @Nullable
  private String parentPath;

  /** 名称（可空） */
  @Schema(description = "名称")
  @Nullable
  private String name;

  /** 路径（可空） */
  @Schema(description = "路径")
  @Nullable
  private String path;

  /** 组件路径（可空） */
  @Schema(description = "组件路径")
  @Nullable
  private String component;

  /** 路由重定向（可空） */
  @Schema(description = "路由重定向")
  @Nullable
  private String redirect;

  /** 许可证（可空） */
  @Schema(description = "许可证")
  @Nullable
  private String license;

  /** 备注（可空） */
  @Schema(description = "备注")
  @Nullable
  private String remark;

  /** 路由元数据 */
  @Schema(description = "路由元数据", requiredMode = Schema.RequiredMode.REQUIRED)
  private MetaResponse meta;
}
