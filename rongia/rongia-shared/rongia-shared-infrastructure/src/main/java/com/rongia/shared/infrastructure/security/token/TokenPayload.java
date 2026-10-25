package com.rongia.shared.infrastructure.security.token;

/**
 * JWT 解析结果，一次性携带会话所需的全部字段
 *
 * @param userId 用户Id
 * @param userName 账号
 * @param jti 令牌唯一标识
 * @param remainingMillis 剩余有效期（毫秒）
 */
public record TokenPayload(long userId, String userName, String jti, long remainingMillis) {}
