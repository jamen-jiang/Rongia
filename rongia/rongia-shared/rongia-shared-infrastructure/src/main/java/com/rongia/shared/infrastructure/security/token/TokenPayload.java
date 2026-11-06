package com.rongia.shared.infrastructure.security.token;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * JWT 一次性解析结果，携带会话所需的全部字段
 *
 * <p>调用方应以 {@link JwtProvider#parsePayload(String)} 一次解析贯穿整个请求，避免对同一令牌反复 parse
 *
 * @param userId 用户Id
 * @param userName 账号
 * @param jti 令牌唯一标识
 * @param sessionId 会话标识（sid claim）。历史令牌不含该 claim 时为 null，调用方按"无会话"降级处理
 * @param remainingMillis 剩余有效期（毫秒）
 */
public record TokenPayload(
    long userId,
    @NonNull String userName,
    @NonNull String jti,
    @Nullable String sessionId,
    long remainingMillis) {}
