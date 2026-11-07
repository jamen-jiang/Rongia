package com.rongia.system.application.signentry.command;

import static com.rongia.system.application.signentry.command.SignEntryHandler.SignEntryIdentity;

import com.rongia.system.application.security.token.SignOutResult;
import com.rongia.system.application.security.token.TokenIssuer;
import com.rongia.system.application.signentry.port.SignEntryRecorder;
import com.rongia.system.domain.signinlog.SignEntryTypeEnum;
import org.axonframework.messaging.commandhandling.annotation.CommandHandler;
import org.springframework.stereotype.Component;

/** 登出命令处理器 */
@Component
public class SignOutCommandHandler {

  /** 令牌颁发端口 */
  private final TokenIssuer tokenIssuer;

  /** 登录入口日志记录端口 */
  private final SignEntryRecorder signEntryRecorder;

  /**
   * 构造器注入
   *
   * @param tokenIssuer 令牌颁发端口
   * @param signEntryRecorder 登录入口日志记录端口
   */
  public SignOutCommandHandler(TokenIssuer tokenIssuer, SignEntryRecorder signEntryRecorder) {
    this.tokenIssuer = tokenIssuer;
    this.signEntryRecorder = signEntryRecorder;
  }

  /**
   * 处理登出命令
   *
   * @param command 登出命令
   */
  @CommandHandler
  public void handle(SignOutCommand command) {
    // 令牌无论成败都记进日志（取不到时由日志记录器退化为上下文令牌）
    String token = command.getToken();
    SignEntryHandler.execute(
        SignEntryTypeEnum.SIGN_OUT.getCode(),
        signEntryRecorder,
        () -> tokenIssuer.revoke(token),
        result -> identity(result, token));
  }

  /**
   * 构造登出日志身份
   *
   * @param result 登出结果（失败时为 null）
   * @param token 令牌
   * @return 日志身份
   */
  private static SignEntryIdentity identity(SignOutResult result, String token) {
    if (result != null) {
      return new SignEntryIdentity(result.userId(), result.userName(), result.name(), token);
    }
    return new SignEntryIdentity(null, null, null, token);
  }
}
