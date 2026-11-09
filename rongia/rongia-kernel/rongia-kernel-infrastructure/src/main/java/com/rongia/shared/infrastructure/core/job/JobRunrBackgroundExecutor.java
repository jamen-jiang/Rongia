package com.rongia.shared.infrastructure.core.job;

import com.rongia.shared.application.cqrs.Command;
import com.rongia.shared.application.port.job.BackgroundExecutor;
import org.axonframework.messaging.commandhandling.gateway.CommandGateway;
import org.jobrunr.scheduling.BackgroundJob;
import org.springframework.stereotype.Component;

/**
 * 基于 JobRunr 的后台执行器实现。
 *
 * <p>把命令通过 JobRunr 提交到后台异步执行，是应用层 {@link BackgroundExecutor} 端口的基础设施适配器。 命令最终由 Axon 的 {@link
 * CommandGateway} 负责分发与处理。
 *
 * <p>JobRunr 会通过 {@code JobActivator} 解析并注入 {@link CommandGateway}， 因此本类无需直接持有网关实例。
 *
 * @see BackgroundExecutor
 * @see BackgroundJob
 */
@Component
public class JobRunrBackgroundExecutor implements BackgroundExecutor {

  /**
   * 提交命令到后台异步执行。
   *
   * <p>命令会被 JobRunr 序列化后入队，由后台任务处理器反序列化并交给 Axon 处理。 因此传入的命令必须是可序列化的，且字段尽量只包含原始类型或可序列化对象。
   *
   * @param command 待执行的命令，不能为 {@code null}
   */
  @Override
  public void dispatch(Command command) {
    BackgroundJob.<CommandGateway>enqueue(gateway -> gateway.sendAndWait(command));
  }
}
