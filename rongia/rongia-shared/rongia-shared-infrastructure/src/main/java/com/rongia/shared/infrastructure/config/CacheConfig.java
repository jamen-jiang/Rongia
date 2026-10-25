package com.rongia.shared.infrastructure.config;

import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

/** 缓存配置 */
@Configuration
@EnableCaching
public class CacheConfig {

  @Bean
  public CacheManager cacheManager(RedisConnectionFactory factory) {
    // 1. 使用 Jackson 3 的 PolymorphicTypeValidator
    PolymorphicTypeValidator typeValidator =
        BasicPolymorphicTypeValidator.builder().allowIfBaseType(Object.class).build();

    // 2. 构建序列化器
    GenericJacksonJsonRedisSerializer serializer =
        GenericJacksonJsonRedisSerializer.builder()
            .enableDefaultTyping(typeValidator) // 传入 Jackson 3 的验证器
            .build();

    // 3. 配置缓存
    RedisCacheConfiguration config =
        RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(serializer))
            .computePrefixWith(cacheName -> "rongia:" + cacheName + "::");

    return RedisCacheManager.builder(factory).cacheDefaults(config).build();
  }
}
