package com.rongia.system.application.menu.command;

import com.rongia.shared.application.cqrs.Command;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 创建菜单命令 */
@NullMarked
@Getter
@Schema(description = "创建菜单命令")
public class AddMenuCommand extends Command {

  /** 类型 */
  @Schema(description = "类型", requiredMode = Schema.RequiredMode.REQUIRED)
  private final String type;

  /** 父Id（可空） */
  @Schema(description = "父Id")
  @Nullable
  private final Long parentId;

  /** 名称（可空） */
  @Schema(description = "名称")
  @Nullable
  private final String name;

  /** 路径（可空） */
  @Schema(description = "路径")
  @Nullable
  private final String path;

  /** 组件路径（可空） */
  @Schema(description = "组件路径")
  @Nullable
  private final String component;

  /** 路由重定向（可空） */
  @Schema(description = "路由重定向")
  @Nullable
  private final String redirect;

  /** 许可证（可空） */
  @Schema(description = "许可证")
  @Nullable
  private final String license;

  /** 备注（可空） */
  @Schema(description = "备注")
  @Nullable
  private final String remark;

  /** 路由元数据 */
  @Schema(description = "路由元数据")
  private final MetaPayload meta;

  /**
   * 构造器
   *
   * @param type 类型
   * @param parentId 父Id（可空）
   * @param name 名称（可空）
   * @param path 路径（可空）
   * @param component 组件路径（可空）
   * @param redirect 路由重定向（可空）
   * @param license 许可证（可空）
   * @param remark 备注（可空）
   * @param meta 路由元数据
   */
  public AddMenuCommand(
      String type,
      @Nullable Long parentId,
      @Nullable String name,
      @Nullable String path,
      @Nullable String component,
      @Nullable String redirect,
      @Nullable String license,
      @Nullable String remark,
      MetaPayload meta) {
    this.type = type;
    this.parentId = parentId;
    this.name = name;
    this.path = path;
    this.component = component;
    this.redirect = redirect;
    this.license = license;
    this.remark = remark;
    this.meta = meta;
  }
}
