package com.rongia.system.application.position.command;

import com.rongia.shared.application.cqrs.Command;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/** 删除岗位命令 */
@NullMarked
@Getter
@Schema(description = "删除岗位命令")
public class RemovePositionCommand extends Command {

  /** 岗位Id */
  @Schema(description = "岗位Id", requiredMode = Schema.RequiredMode.REQUIRED)
  private final Long id;

  /**
   * 构造器
   *
   * @param id 岗位Id
   */
  public RemovePositionCommand(long id) {
    this.id = id;
  }
}
