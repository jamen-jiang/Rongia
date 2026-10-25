package com.rongia.shared.infrastructure.config.axon;

import com.rongia.shared.application.cqrs.NoLogCommand;
import com.rongia.shared.application.port.lock.DistributedLockExecutor;
import com.rongia.shared.application.port.lock.DistributedLockHandle;
import com.rongia.shared.application.security.user.CurrentUser;
import org.axonframework.messaging.commandhandling.CommandMessage;
import org.axonframework.messaging.core.Message;
import org.axonframework.messaging.core.MessageHandlerInterceptor;
import org.axonframework.messaging.core.MessageHandlerInterceptorChain;
import org.axonframework.messaging.core.MessageStream;
import org.axonframework.messaging.core.unitofwork.ProcessingContext;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

/**
 * 命令级分布式锁拦截器
 *
 * <p>按「命令类型 + 用户Id」加锁，防止同一用户并发提交同一命令。
 *
 * <p>通过 {@link DistributedLockExecutor#acquire(String)} 手动获取锁句柄， 并绑定到 {@link MessageStream} 的生命周期上：
 * 正常完成（{@code onComplete}）或异常（{@code onErrorContinue}）时都会释放锁。
 */
@Component
public class DistributedLockInterceptor implements MessageHandlerInterceptor<Message> {

  /** 锁执行器 */
  private final DistributedLockExecutor lockExecutor;

  /** 当前用户 */
  private final CurrentUser currentUser;

  /**
   * 构造器注入
   *
   * @param lockExecutor 锁执行器
   * @param currentUser 当前用户
   */
  public DistributedLockInterceptor(DistributedLockExecutor lockExecutor, CurrentUser currentUser) {
    this.lockExecutor = lockExecutor;
    this.currentUser = currentUser;
  }

  /**
   * 环绕拦截：加锁执行命令
   *
   * @param message 消息
   * @param context 处理上下文
   * @param chain 拦截链
   * @return 消息流
   */
  @Override
  public @NonNull MessageStream<?> interceptOnHandle(
      @NonNull Message message,
      @NonNull ProcessingContext context,
      @NonNull MessageHandlerInterceptorChain<Message> chain) {

    // 1. 不是命令消息：直接放行
    if (!(message instanceof CommandMessage command)) {
      return chain.proceed(message, context);
    }
    // 日志类命令不加锁
    if (command.payload() instanceof NoLogCommand) {
      return chain.proceed(message, context);
    }
    // 2. 构建锁 key：命令类型全名 + 用户Id
    //    例如：com.rongia.application.order.command.SubmitOrderCommand_UserId_1
    String key = command.payloadType().getName() + "_UserId_" + currentUser.getUserId();

    // 3. 手动获取锁（拿到句柄，等流跑完再释放）
    DistributedLockHandle handle = lockExecutor.acquire(key);

    // 4. 命令链返回的流跑完后释放锁
    return chain
        .proceed(message, context)
        // 异常：先释放锁，再让异常继续传播
        .onErrorContinue(
            error -> {
              handle.release();
              return MessageStream.failed(error);
            })
        // 正常完成：释放锁
        .onComplete(handle::release);
  }
}
