package com.rongia.system.application.role.command;

import com.rongia.shared.application.cqrs.Command;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 添加角色命令 */
@NullMarked
@Getter
@Schema(description = "添加角色命令")
public class AddRoleCommand extends Command {

  /** 角色名称 */
  @Schema(description = "角色名称", requiredMode = Schema.RequiredMode.REQUIRED)
  private final String name;

  /** 备注（可空） */
  @Schema(description = "备注")
  @Nullable
  private final String remark;

  /**
   * 构造器
   *
   * @param name 角色名称
   * @param remark 备注（可空）
   */
  public AddRoleCommand(String name, @Nullable String remark) {
    this.name = name;
    this.remark = remark;
  }
}
