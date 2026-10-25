package com.rongia.system.application.user.command;

import com.rongia.shared.application.cqrs.Command;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/** 删除用户命令 */
@NullMarked
@Getter
@Schema(description = "删除用户命令")
public class RemoveUserCommand extends Command {

  /** 用户Id */
  @Schema(description = "用户Id", requiredMode = Schema.RequiredMode.REQUIRED)
  private final Long id;

  /**
   * 构造器
   *
   * @param id 用户Id
   */
  public RemoveUserCommand(long id) {
    this.id = id;
  }
}
