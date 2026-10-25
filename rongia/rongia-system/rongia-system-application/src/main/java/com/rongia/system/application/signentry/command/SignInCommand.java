package com.rongia.system.application.signentry.command;

import com.rongia.shared.application.cqrs.Command;
import com.rongia.shared.application.cqrs.NoLogCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/** 登录命令 */
@NullMarked
@Getter
@Schema(description = "登录命令")
public class SignInCommand extends Command implements NoLogCommand {

  /** 账号 */
  @Schema(description = "账号", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
  private final String userName;

  /** 密码 */
  @Schema(description = "密码", example = "admin2024", requiredMode = Schema.RequiredMode.REQUIRED)
  private final String password;

  /**
   * 构造器
   *
   * @param userName 账号
   * @param password 密码
   */
  public SignInCommand(String userName, String password) {
    this.userName = userName;
    this.password = password;
  }
}
