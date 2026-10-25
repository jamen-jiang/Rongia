package com.rongia.system.application.security.token;

import org.jspecify.annotations.NonNull;

/**
 * 令牌对（携带用户标识，供应用层记日志用）
 *
 * @param userId 用户Id
 * @param userName 账号
 * @param name 用户名称
 * @param accessToken 访问令牌
 * @param refreshToken 刷新令牌
 */
public record TokenPair(
    @NonNull Long userId,
    @NonNull String userName,
    @NonNull String name,
    @NonNull String accessToken,
    @NonNull String refreshToken) {}
