package com.rongia.shared.infrastructure.websocket;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.rongia.shared.infrastructure.security.user.LoginUser;
import com.rongia.shared.infrastructure.security.user.StpLoginUserUtil;
import org.jspecify.annotations.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * STOMP 入站通道认证拦截器。
 *
 * <p>只在 CONNECT 帧时做认证：
 *
 * <ol>
 *   <li>从 CONNECT 帧的 Authorization header 中取 Token
 *   <li>直接查 Sa-Token 的 Redis 映射（STOMP 帧无 HTTP 上下文，走纯 DAO 查询），
 *       未通过按 INVALID / REVOKED 抛 MessagingException，转成前端错误码
 *   <li>从 TokenSession 读登录用户快照，构建 UserPrincipal 并绑定到会话
 * </ol>
 *
 * <p>认证一次后，后续推送通过 Principal 定位用户，无需重复校验。
 */
@Component
public class AuthChannelInterceptor implements ChannelInterceptor {

  /** 认证头名称，前端 CONNECT 帧里传的 */
  private static final String AUTH_HEADER = "Authorization";

  /** Bearer 前缀 */
  private static final String BEARER_PREFIX = "Bearer ";

  @Override
  public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
    StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

    // 只处理 CONNECT 帧，其他帧直接放行
    if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
      return message;
    }

    // 1. 取 Token
    String bearer = accessor.getFirstNativeHeader(AUTH_HEADER);
    if (bearer == null || !bearer.startsWith(BEARER_PREFIX)) {
      throw new MessagingException("Missing or invalid Authorization header");
    }
    String token = bearer.substring(BEARER_PREFIX.length());

    // 2. 查令牌→账号映射：null = 不存在或已过期；负数哨兵 = 被踢下线(-4)/已注销(-5)/被顶下线(-6)
    Object loginId = StpUtil.getLoginIdByToken(token);
    if (loginId == null) {
      // 令牌无效（未登录 / 已过期），前端据此引导重新登录
      throw new MessagingException("TOKEN_INVALID");
    }
    if (loginId instanceof Number number && number.longValue() < 0) {
      throw new MessagingException("TOKEN_REVOKED");
    }

    // 3. 读登录用户快照（登录时由 OpaqueTokenIssuer 写入）
    SaSession session = StpUtil.stpLogic.getTokenSessionByToken(token, false);
    Object value = session == null ? null : session.get(StpLoginUserUtil.LOGIN_USER_KEY);
    if (!(value instanceof LoginUser loginUser)) {
      throw new MessagingException("TOKEN_INVALID");
    }

    // 4. 构建 Principal 并绑定到会话
    accessor.setUser(new UserPrincipal(loginUser.getUserId(), loginUser.getUsername()));

    return message;
  }
}
