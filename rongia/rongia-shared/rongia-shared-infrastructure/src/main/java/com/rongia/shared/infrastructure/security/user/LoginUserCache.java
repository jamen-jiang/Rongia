package com.rongia.shared.infrastructure.security.user;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/** 登录用户缓存实现 */
@Service
public class LoginUserCache {

  /** 缓存 key 前缀 */
  private static final String PREFIX = "login:user:";

  /** 缓存时间：30 分钟 */
  private static final Duration TTL = Duration.ofMinutes(30);

  /** 续期阈值：剩余 TTL 小于该值时才触发续期（减少 Redis 往返） */
  private static final long RENEW_THRESHOLD_MILLIS = Duration.ofMinutes(10).toMillis();

  /** Redis 模板 */
  private final RedisTemplate<String, Object> redisTemplate;

  /**
   * 构造器注入
   *
   * @param redisTemplate Redis 模板
   */
  public LoginUserCache(RedisTemplate<String, Object> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  /**
   * 缓存登录用户
   *
   * @param loginUser 登录用户
   */
  public void put(@NonNull LoginUser loginUser) {
    redisTemplate
        .opsForValue()
        .set(PREFIX + loginUser.getUserId(), LoginUserCacheData.from(loginUser), TTL);
  }

  /**
   * 获取登录用户（命中自动续期）
   *
   * <p>滑动续期策略：只有当剩余 TTL 小于 {@link #RENEW_THRESHOLD_MILLIS} 时才触发 {@code EXPIRE}， 避免每次命中都多一次 Redis
   * 往返。
   *
   * @param userId 用户Id
   * @return 登录用户（可空），不存在返回 null
   */
  @Nullable
  public LoginUser get(long userId) {
    String key = PREFIX + userId;
    Object value = redisTemplate.opsForValue().get(key);
    if (value == null) {
      return null;
    }

    // 剩余 TTL 小于阈值时才续期
    Long remainSeconds = redisTemplate.getExpire(key);
    if (remainSeconds != null
        && remainSeconds > 0
        && remainSeconds * 1000 < RENEW_THRESHOLD_MILLIS) {
      redisTemplate.expire(key, TTL);
    }

    return ((LoginUserCacheData) value).toLoginUser();
  }

  /**
   * 删除登录用户缓存
   *
   * @param userId 用户Id
   */
  public void remove(long userId) {
    redisTemplate.delete(PREFIX + userId);
  }
}
