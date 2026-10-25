package com.rongia.shared.infrastructure.security.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** JWT 工具类实现 */
@Component
@Getter
public class JwtProvider {

  /** userName 的 claim 键名 */
  private static final String CLAIM_USER_NAME = "userName";

  /** 会话标识的 claim 键名：同一次登录的 access / refresh token 共用 */
  private static final String CLAIM_SESSION_ID = "sid";

  /** 签名密钥 */
  private final SecretKey key;

  /** JWT 解析器（复用，避免每次构建） */
  private final JwtParser parser;

  /** 访问令牌过期时间（毫秒） */
  private final long accessTokenExpiration;

  /** 刷新令牌过期时间（毫秒） */
  private final long refreshTokenExpiration;

  /**
   * 构造器注入
   *
   * @param secret 签名密钥
   * @param accessTokenExpiration 访问令牌过期时间
   * @param refreshTokenExpiration 刷新令牌过期时间
   */
  public JwtProvider(
      @Value("${jwt.secret}") String secret,
      @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
      @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.parser = Jwts.parser().verifyWith(key).build();
    this.accessTokenExpiration = accessTokenExpiration;
    this.refreshTokenExpiration = refreshTokenExpiration;
  }

  /**
   * 生成访问令牌
   *
   * <p>约定：access token 的 jti 即会话标识（与 refresh token 的 sid claim 相同）。因此同一会话内 反复刷新时 jti 保持不变：用户 token
   * 集合里一个会话只占一个成员（会话数统计准确），且该会话被 拉黑后其后续令牌一并失效
   *
   * @param userId 用户Id
   * @param userName 账号
   * @param sessionId 会话标识
   * @return JWT
   */
  public @NonNull String generateAccessToken(
      long userId, @NonNull String userName, @NonNull String sessionId) {
    return buildToken(sessionId, userId, userName, sessionId, accessTokenExpiration);
  }

  /**
   * 生成刷新令牌
   *
   * <p>jti 每次生成都是新的随机值，与 access token 的 jti 无关；会话归属由 sid claim 决定
   *
   * @param userId 用户Id
   * @param userName 账号
   * @param sessionId 会话标识（与同次登录的 access token 相同）
   * @return JWT
   */
  public @NonNull String generateRefreshToken(
      long userId, @NonNull String userName, @NonNull String sessionId) {
    return buildToken(
        UUID.randomUUID().toString(), userId, userName, sessionId, refreshTokenExpiration);
  }

  /**
   * 构建令牌：subject 放 userId，userName 与会话标识放 claim
   *
   * @param jti 令牌唯一标识
   * @param userId 用户Id
   * @param userName 账号
   * @param sessionId 会话标识
   * @param expiration 过期毫秒
   * @return JWT
   */
  private @NonNull String buildToken(
      @NonNull String jti,
      long userId,
      @NonNull String userName,
      @NonNull String sessionId,
      long expiration) {
    Date now = new Date();
    return Jwts.builder()
        .id(jti)
        .subject(String.valueOf(userId))
        .claim(CLAIM_USER_NAME, userName)
        .claim(CLAIM_SESSION_ID, sessionId)
        .issuedAt(now)
        .expiration(new Date(now.getTime() + expiration))
        .signWith(key)
        .compact();
  }

  /**
   * 解析令牌
   *
   * @param token JWT
   * @return Claims
   */
  private Claims parseToken(@NonNull String token) {
    return parser.parseSignedClaims(token).getPayload();
  }

  /**
   * 验证令牌是否有效
   *
   * @param token JWT
   * @return true 表示有效
   */
  public boolean isValidToken(@NonNull String token) {
    try {
      parseToken(token);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * 一次性解析令牌，返回会话所需的全部字段
   *
   * @param token JWT
   * @return 解析结果
   */
  public @NonNull TokenPayload parsePayload(@NonNull String token) {
    Claims claims = parseToken(token);
    long remainingMillis = claims.getExpiration().getTime() - System.currentTimeMillis();
    return new TokenPayload(
        Long.parseLong(claims.getSubject()),
        claims.get(CLAIM_USER_NAME, String.class),
        claims.getId(),
        remainingMillis);
  }

  /**
   * 获取用户Id
   *
   * @param token JWT
   * @return 用户Id
   */
  public Long getUserId(@NonNull String token) {
    return Long.valueOf(parseToken(token).getSubject());
  }

  /**
   * 获取账号
   *
   * @param token JWT
   * @return 账号
   */
  public String getUserName(@NonNull String token) {
    return parseToken(token).get(CLAIM_USER_NAME, String.class);
  }

  /**
   * 获取 jti
   *
   * @param token JWT
   * @return jti
   */
  public String getJti(@NonNull String token) {
    return parseToken(token).getId();
  }

  /**
   * 获取会话标识
   *
   * <p>历史令牌（本次改造前签发）不含该 claim，此时返回 null，调用方按"无会话"降级处理
   *
   * @param token JWT
   * @return 会话标识，不存在返回 null
   */
  public String getSessionId(@NonNull String token) {
    return parseToken(token).get(CLAIM_SESSION_ID, String.class);
  }

  /**
   * 获取剩余有效期
   *
   * @param token JWT
   * @return 剩余毫秒
   */
  public long getRemainingMillis(@NonNull String token) {
    Date expiration = parseToken(token).getExpiration();
    return expiration.getTime() - System.currentTimeMillis();
  }
}
