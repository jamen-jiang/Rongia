package com.rongia.shared.infrastructure.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/** 跨域配置 */
@Configuration
public class CorsConfig {

  /**
   * CORS 配置源
   *
   * @return CorsConfigurationSource
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();

    config.addAllowedOrigin("http://localhost:8848"); // ★ 前端地址，写死，别用 *
    // 允许的来源（开发环境用通配符，生产环境要写具体域名）
    config.setAllowedOriginPatterns(List.of("*"));

    // 允许的方法
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

    // 允许的请求头
    config.setAllowedHeaders(List.of("*"));

    // 允许携带 Cookie / Authorization
    config.setAllowCredentials(true);

    // 暴露给前端的响应头
    config.setExposedHeaders(List.of("Content-Disposition", "Authorization"));

    // 预检请求缓存时间（秒）
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
