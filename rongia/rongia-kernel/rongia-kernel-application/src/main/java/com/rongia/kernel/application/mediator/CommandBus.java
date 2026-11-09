package com.rongia.kernel.application.mediator;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 命令总线端口
 *
 * <p>应用层与适配层分发命令的统一入口，具体实现由基础设施层提供（如 Axon 的 CommandGateway）。 调用方只依赖本接口，不感知底层用的是哪个消息框架，换实现时业务代码不用动。
 *
 * <p>本端口只有同步语义：需要异步执行时走 {@link com.rongia.kernel.application.port.job.BackgroundExecutor}
 * 把命令丢到后台，两条路径职责不重叠。
 */
@NullMarked
public interface CommandBus {

  /**
   * 发送命令并等待处理结果
   *
   * @param command 待发送的命令
   * @param resultType 期望的返回值类型
   * @param <R> 返回值类型
   * @return 命令处理器返回的结果
   */
  <R> @Nullable R sendAndWait(Command command, Class<R> resultType);

  /**
   * 发送命令并等待处理完成（不关心返回值）
   *
   * @param command 待发送的命令
   */
  void sendAndWait(Command command);
}
