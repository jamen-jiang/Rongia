package com.rongia.system.application.signentry.command;

import com.rongia.system.application.security.token.SignOutResult;
import com.rongia.system.application.security.token.TokenIssuer;
import com.rongia.system.application.signentry.port.SignEntryRecorder;
import com.rongia.system.domain.signinlog.SignEntryTypeEnum;
import java.time.LocalDateTime;
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
    LocalDateTime startedOn = LocalDateTime.now();
    long startTime = System.currentTimeMillis();

    String refreshToken = command.getRefreshToken();
    Exception exception = null;
    SignOutResult result = null;

    try {
      result = tokenIssuer.revoke(refreshToken);
    } catch (Exception e) {
      exception = e;
      throw e;
    } finally {
      signEntryRecorder.record(
          SignEntryTypeEnum.SIGN_OUT.getCode(),
          result != null ? result.userId() : null,
          result != null ? result.userName() : null,
          result != null ? result.name() : null,
          startedOn,
          LocalDateTime.now(),
          System.currentTimeMillis() - startTime,
          null,
          refreshToken,
          null,
          exception == null,
          exception != null ? exception.getMessage() : null,
          exception != null ? exception.toString() : null);
    }
  }
}
