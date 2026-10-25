package com.rongia.system.application.signentry.command;

import com.rongia.shared.application.cqrs.Command;
import com.rongia.shared.application.cqrs.NoLogCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/** 登出命令 */
@NullMarked
@Getter
@Schema(description = "登出命令")
public class SignOutCommand extends Command implements NoLogCommand {

  /** 刷新令牌 */
  @Schema(description = "刷新令牌", requiredMode = Schema.RequiredMode.REQUIRED)
  private final String refreshToken;

  /**
   * 构造器
   *
   * @param refreshToken 刷新令牌
   */
  public SignOutCommand(String refreshToken) {
    this.refreshToken = refreshToken;
  }
}
