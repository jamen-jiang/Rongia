package com.rongia.system.application.signinlog.command;

import com.rongia.system.domain.signinlog.SignInLog;
import com.rongia.system.domain.signinlog.SignInLogRepository;
import org.axonframework.messaging.commandhandling.annotation.CommandHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 添加登录日志命令处理器 */
@Component
public class AddSignInLogCommandHandler {

  /** 登录日志仓储 */
  private final SignInLogRepository signInLogRepository;

  /**
   * 构造器注入
   *
   * @param signInLogRepository 登录日志仓储
   */
  public AddSignInLogCommandHandler(SignInLogRepository signInLogRepository) {
    this.signInLogRepository = signInLogRepository;
  }

  /**
   * 处理添加登录日志命令
   *
   * @param command 添加登录日志命令
   */
  @CommandHandler
  @Transactional
  public void handle(AddSignInLogCommand command) {
    SignInLog model =
        new SignInLog(
            command.getType(),
            command.getUserId(),
            command.getUserCode(),
            command.getUserName(),
            command.getStartedOn(),
            command.getEndedOn(),
            command.getElapsedMilliseconds(),
            command.getToken(),
            command.getRefreshToken(),
            command.getOldRefreshToken(),
            command.getIp(),
            command.getCity(),
            command.getUserAgent(),
            command.getBrowser(),
            command.getOs(),
            command.getIsSuccess(),
            command.getErrorMessage(),
            command.getException());
    signInLogRepository.save(model);
  }
}
