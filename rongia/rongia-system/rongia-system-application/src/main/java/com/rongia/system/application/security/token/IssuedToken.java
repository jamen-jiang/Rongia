package com.rongia.system.application.security.token;

import org.jspecify.annotations.NonNull;

/**
 * 颁发结果（携带用户标识，供应用层记日志用）
 *
 * @param userId 用户Id
 * @param userName 账号
 * @param name 用户名称
 * @param token 令牌
 */
public record IssuedToken(
    @NonNull Long userId,
    @NonNull String userName,
    @NonNull String name,
    @NonNull String token) {}
