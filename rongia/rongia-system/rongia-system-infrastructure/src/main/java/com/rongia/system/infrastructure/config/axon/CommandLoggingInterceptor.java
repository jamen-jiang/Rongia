package com.rongia.system.infrastructure.config.axon;

import com.rongia.shared.application.cqrs.NoLogCommand;
import com.rongia.shared.application.port.job.BackgroundExecutor;
import com.rongia.shared.application.security.user.CurrentUser;
import com.rongia.shared.infrastructure.core.http.HttpContext;
import com.rongia.shared.infrastructure.core.http.HttpContextHolder;
import com.rongia.system.application.commandlog.command.AddCommandLogCommand;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.messaging.commandhandling.CommandMessage;
import org.axonframework.messaging.core.Message;
import org.axonframework.messaging.core.MessageHandlerInterceptor;
import org.axonframework.messaging.core.MessageHandlerInterceptorChain;
import org.axonframework.messaging.core.MessageStream;
import org.axonframework.messaging.core.unitofwork.ProcessingContext;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 命令日志拦截器
 *
 * <p>记录命令名、耗时、请求/响应、异常。
 *
 * <p>通过 {@link BackgroundExecutor#dispatch} 提交 {@link AddCommandLogCommand} 异步落库， 由 JobRunr
 * 后台执行，最终交给 Axon 的命令处理器写入 {@code CommandLog} 表。
 *
 * <p>注意：{@link AddCommandLogCommand} 实现了 {@link NoLogCommand}，本拦截器会跳过它， 避免「记录日志的命令又被记录」造成无限递归。
 */
@Slf4j
@NullMarked
@Component
public class CommandLoggingInterceptor implements MessageHandlerInterceptor<Message> {

  /** 后台执行器端口 */
  private final BackgroundExecutor backgroundExecutor;

  /** 当前用户 */
  private final CurrentUser currentUser;

  /** JSON 序列化（Spring Boot 自动配置的 Jackson 3 映射器） */
  private final ObjectMapper objectMapper;

  /**
   * 构造器注入
   *
   * @param backgroundExecutor 后台执行器端口
   * @param currentUser 当前用户
   * @param objectMapper JSON 序列化
   */
  public CommandLoggingInterceptor(
      BackgroundExecutor backgroundExecutor,
      CurrentUser currentUser,
      @Qualifier("jacksonJsonMapper") ObjectMapper objectMapper) {
    this.backgroundExecutor = backgroundExecutor;
    this.currentUser = currentUser;
    this.objectMapper = objectMapper;
  }

  /**
   * 环绕拦截：在命令处理前后记录日志
   *
   * @param message 消息
   * @param context 处理上下文
   * @param chain 拦截链
   * @return 消息流
   */
  @Override
  public MessageStream<?> interceptOnHandle(
      Message message, ProcessingContext context, MessageHandlerInterceptorChain<Message> chain) {

    // 1. 不是命令消息：直接放行
    if (!(message instanceof CommandMessage command)) {
      return chain.proceed(message, context);
    }

    // 2. 命令实现了 NoLogCommand：跳过（避免日志命令自己记录自己）
    if (command.payload() instanceof NoLogCommand) {
      return chain.proceed(message, context);
    }

    LocalDateTime startedOn = LocalDateTime.now();
    long startNanos = System.nanoTime();

    // 暂存 onNext 里的响应负载，供 onComplete 使用
    AtomicReference<@Nullable Object> responseRef = new AtomicReference<>();

    // 3. 同步调用 chain.proceed()，捕获同步异常
    MessageStream<?> result;
    try {
      result = chain.proceed(message, context);
    } catch (Throwable e) {
      long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
      sendLog(command, null, startedOn, elapsedMs, e);
      throw e;
    }

    // 4. onNext 拿到 response，onComplete 统一发日志，onErrorContinue 记录异常
    return result
        .onNext(entry -> responseRef.set(entry.message().payload()))
        .onComplete(
            () -> {
              long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
              sendLog(command, responseRef.get(), startedOn, elapsedMs, null);
            })
        .onErrorContinue(
            error -> {
              long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
              sendLog(command, null, startedOn, elapsedMs, error);
              return MessageStream.failed(error);
            });
  }

  /**
   * 发送命令日志命令（异步落库由对应 Handler 负责）
   *
   * @param command 命令
   * @param response 响应（可空）
   * @param startedOn 开始时间
   * @param elapsedMs 耗时（毫秒）
   * @param exception 异常（可空）
   */
  private void sendLog(
      CommandMessage command,
      @Nullable Object response,
      LocalDateTime startedOn,
      long elapsedMs,
      @Nullable Throwable exception) {
    String typeName = command.payloadType().getSimpleName();
    HttpContext httpContext = HttpContextHolder.getContext();
    try {
      Long userId;
      String userName;
      try {
        userId = currentUser.getUserId();
        userName = currentUser.getName();
      } catch (Exception e) {
        // 未登录或匿名请求，用占位值
        userId = null;
        userName = "anonymous";
      }
      AddCommandLogCommand c =
          new AddCommandLogCommand(
              httpContext == null ? null : httpContext.getTraceId(),
              userId,
              userName,
              startedOn,
              LocalDateTime.now(),
              elapsedMs,
              typeName,
              typeName,
              serialize(command.payload()),
              serialize(response),
              exception == null,
              exception == null ? null : buildErrorMessage(exception),
              exception == null ? null : stackTraceToString(exception));
      backgroundExecutor.dispatch(c);
    } catch (Exception e) {
      // 日志失败不能影响业务
      log.warn("命令日志发送失败", e);
    }
  }

  /**
   * 把异常链拼成一行（Axon → Handler → 业务 → DB）
   *
   * @param t 异常
   * @return 异常链 message
   */
  private String buildErrorMessage(Throwable t) {
    StringBuilder sb = new StringBuilder();
    Throwable cur = t;
    while (cur != null) {
      if (!sb.isEmpty()) {
        sb.append(" -> ");
      }
      sb.append(cur.getClass().getSimpleName()).append(": ").append(cur.getMessage());
      if (cur.getCause() == cur) {
        break;
      }
      cur = cur.getCause();
    }
    return sb.toString();
  }

  /**
   * 异常堆栈转字符串（不截断）
   *
   * @param throwable 异常
   * @return 堆栈
   */
  private String stackTraceToString(Throwable throwable) {
    StringWriter sw = new StringWriter();
    throwable.printStackTrace(new PrintWriter(sw));
    return sw.toString();
  }

  /**
   * 序列化，"{}" 视为 null
   *
   * @param obj 对象
   * @return JSON 字符串（可空）
   */
  @Nullable
  private String serialize(@Nullable Object obj) {
    if (obj == null) {
      return null;
    }
    try {
      String json = objectMapper.writeValueAsString(obj);
      return "{}".equals(json.trim()) ? null : json;
    } catch (Exception e) {
      return null;
    }
  }
}
