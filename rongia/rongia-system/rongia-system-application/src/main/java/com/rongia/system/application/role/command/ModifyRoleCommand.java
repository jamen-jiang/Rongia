package com.rongia.system.application.role.command;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 修改角色命令 */
@NullMarked
@Getter
@Schema(description = "修改角色命令")
public class ModifyRoleCommand extends AddRoleCommand {

  /** 角色Id */
  @Schema(description = "角色Id", requiredMode = Schema.RequiredMode.REQUIRED)
  private final Long id;

  /**
   * 构造器
   *
   * @param id 角色Id
   * @param name 角色名称
   * @param remark 备注（可空）
   */
  public ModifyRoleCommand(long id, String name, @Nullable String remark) {
    super(name, remark);
    this.id = id;
  }
}
