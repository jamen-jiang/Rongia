package com.rongia.system.application.signentry.command;

import com.rongia.system.application.signentry.port.SignEntryRecord;
import com.rongia.system.application.signentry.port.SignEntryRecorder;
import java.time.LocalDateTime;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 登录入口命令处理器模板：统一"计时 - 执行 - 记录日志"骨架，子类只提供业务动作与日志身份降级
 *
 * <p>日志在 finally 中记录：无论业务成败，登录 / 登出都会留下一条日志，与原行为一致
 */
public final class SignEntryHandler {

  private SignEntryHandler() {}

  /**
   * 日志身份：本次日志归属的用户与令牌快照
   *
   * @param userId 用户Id（可空）
   * @param userName 用户名（可空）
   * @param givenName 用户昵称（可空）
   * @param token 令牌（可空）
   */
  public record SignEntryIdentity(
      @Nullable Long userId,
      @Nullable String userName,
      @Nullable String givenName,
      @Nullable String token) {}

  /**
   * 执行业务动作并在 finally 中记录日志
   *
   * <p>identity 函数永不为 null：动作成功时从结果构造，失败时由调用方的 lambda 降级
   * （例如登录失败时把命令里输入的用户名记进日志）
   *
   * @param <T> 业务结果类型
   * @param type 日志类型：SignIn / SignOut
   * @param recorder 登录入口日志记录端口
   * @param action 业务动作
   * @param identity 从业务结果构造日志身份（失败时 result 为 null，由 lambda 降级）
   * @return 业务结果
   */
  public static <T> T execute(
      @NonNull String type,
      @NonNull SignEntryRecorder recorder,
      Supplier<T> action,
      Function<T, SignEntryIdentity> identity) {
    LocalDateTime startedOn = LocalDateTime.now();
    long startTime = System.currentTimeMillis();

    Exception exception = null;
    T result = null;

    try {
      result = action.get();
      return result;
    } catch (Exception e) {
      exception = e;
      throw e;
    } finally {
      SignEntryIdentity id = identity.apply(result);
      recorder.record(
          new SignEntryRecord(
              type,
              id.userId(),
              id.userName(),
              id.givenName(),
              startedOn,
              LocalDateTime.now(),
              System.currentTimeMillis() - startTime,
              id.token(),
              exception == null,
              exception != null ? exception.getMessage() : null,
              exception != null ? exception.toString() : null));
    }
  }
}
