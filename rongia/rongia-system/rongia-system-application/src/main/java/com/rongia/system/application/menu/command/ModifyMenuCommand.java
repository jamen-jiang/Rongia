package com.rongia.system.application.menu.command;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 修改菜单命令 */
@NullMarked
@Getter
@Schema(description = "修改菜单命令")
public class ModifyMenuCommand extends AddMenuCommand {

  /** 菜单Id */
  @Schema(description = "菜单Id", requiredMode = Schema.RequiredMode.REQUIRED)
  private final Long id;

  /**
   * 构造器
   *
   * @param id 菜单Id
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
  public ModifyMenuCommand(
      long id,
      String type,
      @Nullable Long parentId,
      @Nullable String name,
      @Nullable String path,
      @Nullable String component,
      @Nullable String redirect,
      @Nullable String license,
      @Nullable String remark,
      MetaPayload meta) {
    super(type, parentId, name, path, component, redirect, license, remark, meta);
    this.id = id;
  }
}
