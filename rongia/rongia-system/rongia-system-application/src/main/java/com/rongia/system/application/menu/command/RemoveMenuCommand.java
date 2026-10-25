package com.rongia.system.application.menu.command;

import com.rongia.shared.application.cqrs.Command;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/** 删除菜单命令 */
@NullMarked
@Getter
@Schema(description = "删除菜单命令")
public class RemoveMenuCommand extends Command {

  /** 菜单Id */
  @Schema(description = "菜单Id", requiredMode = Schema.RequiredMode.REQUIRED)
  private final Long id;

  /**
   * 构造器
   *
   * @param id 菜单Id
   */
  public RemoveMenuCommand(long id) {
    this.id = id;
  }
}
