package com.rongia.kernel.infrastructure.mediator;

import com.rongia.kernel.application.mediator.Command;
import com.rongia.kernel.application.mediator.CommandBus;
import org.axonframework.messaging.commandhandling.gateway.CommandGateway;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * 命令总线实现（基于 Axon 的 {@link CommandGateway}）
 *
 * <p>纯委托：不加工命令、不改返回值、不吞异常，Axon 抛什么就往上抛什么，由 {@code GlobalExceptionHandler} 统一翻译成对外错误码。
 *
 * <p>注入的就是 {@code SignEntryController} 之前直接使用的那个 gateway bean，因此换成端口后行为完全等价。
 */
@NullMarked
@Component
public class AxonCommandBus implements CommandBus {

  /** Axon 命令网关 */
  private final CommandGateway commandGateway;

  /**
   * 构造器注入
   *
   * @param commandGateway Axon 命令网关
   */
  public AxonCommandBus(CommandGateway commandGateway) {
    this.commandGateway = commandGateway;
  }

  /**
   * 发送命令并等待处理结果
   *
   * @param command 待发送的命令
   * @param resultType 期望的返回值类型
   * @param <R> 返回值类型
   * @return 命令处理器返回的结果
   */
  @Override
  public <R> @Nullable R sendAndWait(Command command, Class<R> resultType) {
    return commandGateway.sendAndWait(command, resultType);
  }

  /**
   * 发送命令并等待处理完成（不关心返回值）
   *
   * @param command 待发送的命令
   */
  @Override
  public void sendAndWait(Command command) {
    commandGateway.sendAndWait(command);
  }
}
