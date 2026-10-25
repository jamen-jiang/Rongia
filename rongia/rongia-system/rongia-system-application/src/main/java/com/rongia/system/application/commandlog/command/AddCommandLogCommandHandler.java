package com.rongia.system.application.commandlog.command;

import com.rongia.system.domain.commandlog.CommandLog;
import com.rongia.system.domain.commandlog.CommandLogRepository;
import org.axonframework.messaging.commandhandling.annotation.CommandHandler;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 添加命令日志命令处理器
 *
 * <p>由 {@code CommandLoggingInterceptor} 异步触发，将命令审计信息落库。
 */
@NullMarked
@Component
public class AddCommandLogCommandHandler {

  /** 命令日志仓储 */
  private final CommandLogRepository commandLogRepository;

  /**
   * 构造器注入
   *
   * @param commandLogRepository 命令日志仓储
   */
  public AddCommandLogCommandHandler(CommandLogRepository commandLogRepository) {
    this.commandLogRepository = commandLogRepository;
  }

  /**
   * 处理添加命令日志命令
   *
   * @param command 添加命令日志命令
   */
  @CommandHandler
  @Transactional
  public void handle(AddCommandLogCommand command) {
    CommandLog model =
        new CommandLog(
            command.getTraceId(),
            command.getUserId(),
            command.getUserName(),
            command.getStartedOn(),
            command.getEndedOn(),
            command.getElapsedMilliseconds(),
            command.getName(),
            command.getTypeName(),
            command.getRequest(),
            command.getResponse(),
            command.getIsSuccess(),
            command.getErrorMessage(),
            command.getException());
    commandLogRepository.save(model);
  }
}
