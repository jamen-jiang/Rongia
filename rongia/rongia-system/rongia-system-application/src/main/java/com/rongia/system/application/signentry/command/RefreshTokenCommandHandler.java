package com.rongia.system.application.signentry.command;

import com.rongia.system.application.security.token.TokenIssuer;
import com.rongia.system.application.security.token.TokenPair;
import com.rongia.system.application.signentry.port.SignEntryRecorder;
import com.rongia.system.domain.signinlog.SignEntryTypeEnum;
import java.time.LocalDateTime;
import org.axonframework.messaging.commandhandling.annotation.CommandHandler;
import org.springframework.stereotype.Component;

/** 刷新令牌命令处理器 */
@Component
public class RefreshTokenCommandHandler {

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
  public RefreshTokenCommandHandler(TokenIssuer tokenIssuer, SignEntryRecorder signEntryRecorder) {
    this.tokenIssuer = tokenIssuer;
    this.signEntryRecorder = signEntryRecorder;
  }

  /**
   * 处理刷新令牌命令
   *
   * @param command 刷新令牌命令
   * @return 登录响应
   */
  @CommandHandler
  public SignInResponse handle(RefreshTokenCommand command) {
    LocalDateTime startedOn = LocalDateTime.now();
    long startTime = System.currentTimeMillis();

    String oldRefreshToken = command.getRefreshToken();
    Exception exception = null;
    TokenPair tokens = null;

    try {
      tokens = tokenIssuer.refresh(oldRefreshToken);
      return new SignInResponse(tokens.accessToken(), tokens.refreshToken());
    } catch (Exception e) {
      exception = e;
      throw e;
    } finally {
      signEntryRecorder.record(
          SignEntryTypeEnum.REFRESH_TOKEN.getCode(),
          tokens != null ? tokens.userId() : null,
          tokens != null ? tokens.userName() : null,
          tokens != null ? tokens.name() : null,
          startedOn,
          LocalDateTime.now(),
          System.currentTimeMillis() - startTime,
          tokens != null ? tokens.accessToken() : null,
          tokens != null ? tokens.refreshToken() : null,
          oldRefreshToken,
          exception == null,
          exception != null ? exception.getMessage() : null,
          exception != null ? exception.toString() : null);
    }
  }
}
