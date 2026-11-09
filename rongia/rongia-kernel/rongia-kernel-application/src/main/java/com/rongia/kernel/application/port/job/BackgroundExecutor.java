package com.rongia.kernel.application.port.job;

import com.rongia.kernel.application.mediator.Command;

/**
 * 后台执行器端口
 *
 * <p>应用层用于把命令提交到后台异步执行的抽象，具体实现由基础设施层提供（如 JobRunr）。
 */
public interface BackgroundExecutor {

  /**
   * 提交命令到后台执行
   *
   * @param command 待执行的命令，需可被序列化
   */
  void dispatch(Command command);
}
