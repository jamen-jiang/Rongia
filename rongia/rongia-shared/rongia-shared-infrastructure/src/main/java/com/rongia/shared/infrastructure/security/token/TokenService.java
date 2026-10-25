package com.rongia.shared.infrastructure.security.token;

import com.rongia.shared.infrastructure.core.http.HttpContext;
import com.rongia.shared.infrastructure.core.http.HttpContextHolder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/** 令牌 Redis 管理实现 */
@Service
public class TokenService {

  /** 黑名单前缀 */
  private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

  /** 刷新令牌前缀（按会话隔离：jwt:refresh:{userId}:{sessionId}） */
  private static final String REFRESH_PREFIX = "jwt:refresh:";

  /**
   * 用户 token 集合前缀：{userId} -> {jti1, jti2, ...}
   *
   * <p>access token 的 jti 即会话标识，故该集合同时充当"用户会话集合"：一个成员 = 一个在线端
   */
  private static final String USER_TOKENS_PREFIX = "jwt:user:tokens:";

  /** 用户登录元信息前缀：{userId} -> {username, clientIp, loginAt} */
  private static final String USER_META_PREFIX = "jwt:user:meta:";

  /** 全局在线用户集合 */
  private static final String ONLINE_USERS_KEY = "jwt:online:users";

  /** 踢人时黑名单兜底 TTL（应 ≥ JWT 最长有效期） */
  private static final long KICK_FALLBACK_TTL_MILLIS = Duration.ofDays(7).toMillis();

  /** Redis 模板 */
  private final RedisTemplate<String, Object> redisTemplate;

  /** JWT 工具类 */
  private final JwtProvider jwtProvider;

  /**
   * 构造器注入
   *
   * @param redisTemplate Redis 模板
   * @param jwtProvider JWT 工具类
   */
  public TokenService(RedisTemplate<String, Object> redisTemplate, JwtProvider jwtProvider) {
    this.redisTemplate = redisTemplate;
    this.jwtProvider = jwtProvider;
  }

  // ==================== 黑名单 ====================

  /**
   * 加入黑名单
   *
   * @param jti 令牌唯一标识
   * @param remainingMillis 剩余有效期（毫秒）
   */
  public void addToBlacklist(@NonNull String jti, long remainingMillis) {
    if (remainingMillis > 0) {
      // setIfAbsent：key 不存在时才写入，避免同一 jti 被重复覆盖
      redisTemplate
          .opsForValue()
          .setIfAbsent(BLACKLIST_PREFIX + jti, "revoked", Duration.ofMillis(remainingMillis));
    }
  }

  /**
   * 判断是否在黑名单
   *
   * @param jti 令牌唯一标识
   * @return true 表示已失效
   */
  public boolean isBlacklisted(@NonNull String jti) {
    return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + jti));
  }

  // ==================== 刷新令牌 ====================

  /**
   * 存储刷新令牌（按会话隔离，同一用户可多端同时在线）
   *
   * @param userId 用户Id
   * @param sessionId 会话标识
   * @param refreshToken 刷新令牌
   * @param expirationMillis 过期毫秒
   */
  public void storeRefreshToken(
      long userId,
      @NonNull String sessionId,
      @NonNull String refreshToken,
      long expirationMillis) {
    redisTemplate
        .opsForValue()
        .set(refreshKey(userId, sessionId), refreshToken, Duration.ofMillis(expirationMillis));
  }

  /**
   * 获取指定会话的刷新令牌
   *
   * @param userId 用户Id
   * @param sessionId 会话标识
   * @return 刷新令牌，不存在返回 null
   */
  @Nullable
  public String getRefreshToken(long userId, @NonNull String sessionId) {
    Object value = redisTemplate.opsForValue().get(refreshKey(userId, sessionId));
    return value == null ? null : value.toString();
  }

  /**
   * 删除指定会话的刷新令牌（仅影响该会话，其他端不受影响）
   *
   * @param userId 用户Id
   * @param sessionId 会话标识
   */
  public void removeRefreshToken(long userId, @NonNull String sessionId) {
    redisTemplate.delete(refreshKey(userId, sessionId));
  }

  /**
   * 删除该用户所有会话的刷新令牌（踢人下线时使用）
   *
   * <p>会话标识即 token 集合里的 jti，直接遍历即可定位每个会话的刷新令牌
   *
   * @param userId 用户Id
   */
  public void removeAllRefreshTokens(long userId) {
    Set<Object> sessionIds = redisTemplate.opsForSet().members(USER_TOKENS_PREFIX + userId);
    if (sessionIds != null) {
      for (Object sessionId : sessionIds) {
        redisTemplate.delete(refreshKey(userId, sessionId.toString()));
      }
    }
  }

  /**
   * 刷新令牌 Redis key（按会话隔离）
   *
   * @param userId 用户Id
   * @param sessionId 会话标识
   * @return Redis key
   */
  private String refreshKey(long userId, @NonNull String sessionId) {
    return REFRESH_PREFIX + userId + ":" + sessionId;
  }

  // ==================== 登录登记 ====================

  /**
   * 登记用户的一次登录（登录成功后调用）
   *
   * @param userId 用户 ID
   * @param username 用户名
   * @param jti access token 的唯一标识（即会话标识）
   * @param clientIp 客户端 IP
   * @param expireMillis 会话存活时长（毫秒）。应传刷新令牌有效期而非 access token 有效期，
   *     否则会话会在 access token 过期后消失，"踢人下线"将漏掉仅剩刷新令牌有效的会话
   */
  public void registerLogin(
      long userId,
      String username,
      @NonNull String jti,
      @Nullable String clientIp,
      long expireMillis) {
    // 1. 把 jti 加到用户的 token 集合
    String tokensKey = USER_TOKENS_PREFIX + userId;
    redisTemplate.opsForSet().add(tokensKey, jti);
    redisTemplate.expire(tokensKey, Duration.ofMillis(expireMillis));

    // 2. 记录用户登录元信息
    String metaKey = USER_META_PREFIX + userId;
    Map<String, Object> meta = new HashMap<>(4);
    meta.put("userId", userId);
    meta.put("username", username);
    meta.put("clientIp", clientIp == null ? "" : clientIp);
    meta.put("loginAt", System.currentTimeMillis());
    redisTemplate.opsForHash().putAll(metaKey, meta);
    redisTemplate.expire(metaKey, Duration.ofMillis(expireMillis));

    // 3. 加入全局在线用户集合
    redisTemplate.opsForSet().add(ONLINE_USERS_KEY, userId);
  }

  /**
   * 延长会话在 Redis 中的存活时间（刷新令牌时调用）
   *
   * <p>会话集合与元信息的 TTL 原本按 access token 有效期设置，而刷新令牌有效期更长。若不延长，
   * 会话会在 access token 过期后从集合中消失，导致"在线列表"丢人、"踢人下线"漏掉该会话
   *
   * @param userId 用户 ID
   * @param sessionId 会话标识（即 access token 的 jti）
   * @param expireMillis 新的存活时长（毫秒，通常为刷新令牌有效期）
   */
  public void extendSession(long userId, @NonNull String sessionId, long expireMillis) {
    Duration ttl = Duration.ofMillis(expireMillis);

    String tokensKey = USER_TOKENS_PREFIX + userId;
    redisTemplate.opsForSet().add(tokensKey, sessionId);
    redisTemplate.expire(tokensKey, ttl);

    redisTemplate.expire(USER_META_PREFIX + userId, ttl);

    redisTemplate.opsForSet().add(ONLINE_USERS_KEY, userId);
  }

  /**
   * 注销单次登录（用户主动登出时调用）
   *
   * @param userId 用户 ID
   * @param jti access token 的唯一标识
   */
  public void unregisterLogin(long userId, @NonNull String jti) {
    String tokensKey = USER_TOKENS_PREFIX + userId;
    redisTemplate.opsForSet().remove(tokensKey, jti);

    // 该用户没有其他有效 token 时，清理在线状态
    Long size = redisTemplate.opsForSet().size(tokensKey);
    if (size == null || size == 0) {
      redisTemplate.opsForSet().remove(ONLINE_USERS_KEY, userId);
      redisTemplate.delete(USER_META_PREFIX + userId);
    }
  }

  // ==================== 踢人下线 ====================

  /**
   * 踢人下线：强制该用户所有 token 失效
   *
   * @param userId 用户 ID
   * @return 被踢下线的会话数
   */
  public int kickUser(long userId) {
    String tokensKey = USER_TOKENS_PREFIX + userId;

    // 1. 取出该用户所有 jti
    Set<Object> jtis = redisTemplate.opsForSet().members(tokensKey);
    if (jtis == null || jtis.isEmpty()) {
      return 0;
    }

    // 2. 全部加入黑名单（用兜底 TTL，到期后自动清除）
    for (Object jti : jtis) {
      addToBlacklist(jti.toString(), KICK_FALLBACK_TTL_MILLIS);
    }

    // 3. 清理刷新令牌（必须在删除会话集合之前，否则拿不到 jti 列表）
    removeAllRefreshTokens(userId);

    // 4. 清理会话集合、元信息
    redisTemplate.delete(tokensKey);
    redisTemplate.delete(USER_META_PREFIX + userId);

    // 5. 从在线列表移除
    redisTemplate.opsForSet().remove(ONLINE_USERS_KEY, userId);

    return jtis.size();
  }

  /**
   * 撤销当前请求的 accessToken：拉黑 + 从会话集合移除
   *
   * <p>语义等价于"拉黑 + 取消登记"，会话集合为空时自动清理在线列表
   *
   * <p>黑名单时长用兜底 TTL 而非当前 token 的剩余时长：jti 即会话标识，同一会话内先签发的
   * access token 可能比当前这个剩余更久，按当前剩余拉黑会让旧令牌在会话结束前重新可用
   */
  public void revokeCurrentAccessToken() {
    HttpContext httpContext = HttpContextHolder.getContext();
    if (httpContext == null) {
      return;
    }
    String token = httpContext.getToken();
    if (token == null || !jwtProvider.isValidToken(token)) {
      return;
    }

    // 1. 取 userId 和 jti
    Long userId = jwtProvider.getUserId(token);
    String jti = jwtProvider.getJti(token);

    // 2. 拉黑 accessToken（兜底 TTL ≥ JWT 最长有效期）
    addToBlacklist(jti, KICK_FALLBACK_TTL_MILLIS);

    // 3. 从会话集合移除（集合空了会自动清理在线列表和 meta）
    unregisterLogin(userId, jti);
  }

  // ==================== 在线列表 ====================

  /**
   * 获取在线用户列表
   *
   * @return 在线用户信息列表
   */
  public List<Map<String, Object>> listOnlineUsers() {
    Set<Object> userIds = redisTemplate.opsForSet().members(ONLINE_USERS_KEY);
    if (userIds == null || userIds.isEmpty()) {
      return Collections.emptyList();
    }

    List<Map<String, Object>> result = new ArrayList<>(userIds.size());
    for (Object uid : userIds) {
      Map<String, Object> meta = loadUserMeta(uid.toString());
      if (meta != null) {
        // 附带该用户当前会话数
        Long sessionCount = redisTemplate.opsForSet().size(USER_TOKENS_PREFIX + uid);
        meta.put("sessionCount", sessionCount == null ? 0 : sessionCount);
        result.add(meta);
      }
    }
    return result;
  }

  /**
   * 获取指定用户的在线会话
   *
   * @param userId 用户 ID
   * @return 会话信息列表
   */
  public List<Map<String, Object>> listUserSessions(long userId) {
    Set<Object> jtis = redisTemplate.opsForSet().members(USER_TOKENS_PREFIX + userId);
    if (jtis == null || jtis.isEmpty()) {
      return Collections.emptyList();
    }

    Map<String, Object> meta = loadUserMeta(String.valueOf(userId));
    return jtis.stream()
        .map(
            jti -> {
              Map<String, Object> session = new HashMap<>(8);
              session.put("jti", jti.toString());
              session.put("userId", userId);
              if (meta != null) {
                session.putAll(meta);
              }
              return session;
            })
        .collect(Collectors.toList());
  }

  // ==================== 私有工具 ====================

  /**
   * 读取用户登录元信息
   *
   * @param userId 用户 ID
   * @return 元信息 Map，不存在返回 null
   */
  @Nullable
  private Map<String, Object> loadUserMeta(String userId) {
    Map<Object, Object> raw = redisTemplate.opsForHash().entries(USER_META_PREFIX + userId);
    if (raw == null || raw.isEmpty()) {
      return null;
    }
    Map<String, Object> meta = new HashMap<>(raw.size());
    raw.forEach((k, v) -> meta.put(k.toString(), v));
    return meta;
  }
}
