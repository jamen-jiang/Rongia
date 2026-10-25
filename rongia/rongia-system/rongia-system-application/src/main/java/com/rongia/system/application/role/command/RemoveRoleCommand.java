package com.rongia.system.application.role.command;

import com.rongia.shared.application.cqrs.Command;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/** 删除角色命令 */
@NullMarked
@Getter
@Schema(description = "删除角色命令")
public class RemoveRoleCommand extends Command {

  /** 角色Id */
  @Schema(description = "角色Id", requiredMode = Schema.RequiredMode.REQUIRED)
  private final Long id;

  /**
   * 构造器
   *
   * @param id 角色Id
   */
  public RemoveRoleCommand(long id) {
    this.id = id;
  }
}
