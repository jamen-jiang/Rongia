package com.rongia.system.application.signentry.command;

import static com.rongia.system.application.signentry.command.SignEntryHandler.SignEntryIdentity;

import com.rongia.system.application.security.token.IssuedToken;
import com.rongia.system.application.security.token.TokenIssuer;
import com.rongia.system.application.signentry.port.SignEntryRecorder;
import com.rongia.system.domain.signinlog.SignEntryTypeEnum;
import org.axonframework.messaging.commandhandling.annotation.CommandHandler;
import org.springframework.stereotype.Component;

/** 登录命令处理器 */
@Component
public class SignInCommandHandler {

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
  public SignInCommandHandler(TokenIssuer tokenIssuer, SignEntryRecorder signEntryRecorder) {
    this.tokenIssuer = tokenIssuer;
    this.signEntryRecorder = signEntryRecorder;
  }

  /**
   * 处理登录命令
   *
   * @param command 登录命令
   * @return 登录响应
   */
  @CommandHandler
  public SignInResponse handle(SignInCommand command) {
    IssuedToken issued =
        SignEntryHandler.execute(
            SignEntryTypeEnum.SIGN_IN.getCode(),
            signEntryRecorder,
            () -> tokenIssuer.issue(command.getUserName(), command.getPassword()),
            result ->
                result != null
                    ? new SignEntryIdentity(
                        result.userId(), result.userName(), result.name(), result.token())
                    // 登录失败时把命令里输入的用户名记进日志
                    : new SignEntryIdentity(null, command.getUserName(), null, null));
    return new SignInResponse(issued.token());
  }
}
