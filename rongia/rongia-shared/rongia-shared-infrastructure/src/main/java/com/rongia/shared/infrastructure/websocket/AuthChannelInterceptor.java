package com.rongia.shared.infrastructure.websocket;

import com.rongia.shared.infrastructure.security.token.JwtProvider;
import com.rongia.shared.infrastructure.security.token.TokenPayload;
import com.rongia.shared.infrastructure.security.token.TokenService;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * STOMP 入站通道认证拦截器。
 *
 * <p>只在 CONNECT 帧时做认证：
 *
 * <ol>
 *   <li>从 CONNECT 帧的 Authorization header 中取 Token
 *   <li>JwtProvider 解析并校验签名、有效期
 *   <li>TokenService 查 Redis 黑名单，确认未被吊销
 *   <li>构建 UserPrincipal 并绑定到会话
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

  private final JwtProvider jwtProvider;
  private final TokenService tokenService;

  public AuthChannelInterceptor(JwtProvider jwtProvider, TokenService tokenService) {
    this.jwtProvider = jwtProvider;
    this.tokenService = tokenService;
  }

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

    // 2. 解析 Token（校验签名与有效期）
    TokenPayload payload;
    try {
      payload = jwtProvider.parsePayload(token);
    } catch (Exception e) {
      // 前端收到 TOKEN_INVALID，会触发 axios 拦截器去刷新 Token
      throw new MessagingException("TOKEN_INVALID");
    }

    // 3. 查黑名单，确认 Token 未被吊销
    if (tokenService.isBlacklisted(payload.jti())) {
      throw new MessagingException("TOKEN_REVOKED");
    }

    // 4. 构建 Principal 并绑定到会话
    UserPrincipal user = new UserPrincipal(payload.userId(), payload.userName());
    accessor.setUser(new UsernamePasswordAuthenticationToken(user, null, List.of()));

    return message;
  }
}
