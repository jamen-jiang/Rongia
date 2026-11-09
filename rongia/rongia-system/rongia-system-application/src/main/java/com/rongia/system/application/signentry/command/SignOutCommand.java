package com.rongia.system.application.signentry.command;

import com.rongia.kernel.application.mediator.Command;
import com.rongia.kernel.application.mediator.NoLogCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/** 登出命令 */
@NullMarked
@Getter
@Schema(description = "登出命令")
public class SignOutCommand extends Command implements NoLogCommand {

  /** 令牌 */
  @Schema(description = "令牌", requiredMode = Schema.RequiredMode.REQUIRED)
  private final String token;

  /**
   * 构造器
   *
   * @param token 令牌
   */
  public SignOutCommand(String token) {
    this.token = token;
  }
}
