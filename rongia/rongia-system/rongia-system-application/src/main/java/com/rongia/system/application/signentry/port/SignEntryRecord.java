package com.rongia.system.application.signentry.port;

import java.time.LocalDateTime;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 一条登录入口日志：登录 / 登出两个场景共用的记录载体
 *
 * @param type 类型：SignIn / SignOut
 * @param userId 用户Id（可空）
 * @param userName 输入的用户名（可空）
 * @param givenName 用户昵称（可空）
 * @param startedOn 开始时间
 * @param endedOn 结束时间
 * @param elapsedMilliseconds 耗时（毫秒）
 * @param token 令牌（可空）
 * @param success 是否成功
 * @param errorMessage 错误信息（可空）
 * @param exception 异常信息（可空）
 */
public record SignEntryRecord(
    @NonNull String type,
    @Nullable Long userId,
    @Nullable String userName,
    @Nullable String givenName,
    @NonNull LocalDateTime startedOn,
    @NonNull LocalDateTime endedOn,
    long elapsedMilliseconds,
    @Nullable String token,
    boolean success,
    @Nullable String errorMessage,
    @Nullable String exception) {}
