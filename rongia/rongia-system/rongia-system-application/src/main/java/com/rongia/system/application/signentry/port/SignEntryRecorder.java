package com.rongia.system.application.signentry.port;

import java.time.LocalDateTime;
import org.jspecify.annotations.NonNull;

/** 登录入口日志记录端口 */
public interface SignEntryRecorder {

  /**
   * 记录一条登录入口日志
   *
   * @param type 类型：SignIn / RefreshToken / SignOut
   * @param userId 用户Id（可空）
   * @param userName 输入的用户名（可空）
   * @param givenName 用户昵称（可空）
   * @param startedOn 开始时间
   * @param endedOn 结束时间
   * @param elapsedMilliseconds 耗时（毫秒）
   * @param accessToken 访问令牌（可空）
   * @param refreshToken 刷新令牌（可空）
   * @param oldRefreshToken 旧刷新令牌（可空，仅刷新令牌场景使用）
   * @param success 是否成功
   * @param errorMessage 错误信息（可空）
   * @param exception 异常信息（可空）
   */
  void record(
      @NonNull String type,
      Long userId,
      String userName,
      String givenName,
      @NonNull LocalDateTime startedOn,
      @NonNull LocalDateTime endedOn,
      long elapsedMilliseconds,
      String accessToken,
      String refreshToken,
      String oldRefreshToken,
      boolean success,
      String errorMessage,
      String exception);
}
