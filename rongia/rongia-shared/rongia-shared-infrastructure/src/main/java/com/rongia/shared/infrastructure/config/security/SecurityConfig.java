package com.rongia.shared.infrastructure.config.security;

import com.rongia.shared.infrastructure.security.CompatiblePasswordEncoder;
import com.rongia.shared.infrastructure.security.filter.JwtAuthenticationFilter;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/** Spring Security 配置类（适配 Spring Boot 4 / Security 7） */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(jsr250Enabled = true) // ★ 必须开启 JSR-250，否则 @PermitAll 不生效
public class SecurityConfig {

  /** JWT 认证过滤器 */
  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  /** CORS 配置源 */
  private final CorsConfigurationSource corsConfigurationSource;

  /** {@code @PermitAll} URL 扫描器 */
  private final PermitAllUrlScanner permitAllUrlScanner;

  /**
   * 构造器注入
   *
   * @param jwtAuthenticationFilter JWT 认证过滤器
   * @param corsConfigurationSource CORS 配置源
   * @param permitAllUrlScanner @PermitAll URL 扫描器
   */
  public SecurityConfig(
      JwtAuthenticationFilter jwtAuthenticationFilter,
      CorsConfigurationSource corsConfigurationSource,
      PermitAllUrlScanner permitAllUrlScanner) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.corsConfigurationSource = corsConfigurationSource;
    this.permitAllUrlScanner = permitAllUrlScanner;
  }

  /**
   * 安全过滤器链配置
   *
   * @param http HttpSecurity 对象
   * @return SecurityFilterChain
   * @throws Exception 配置异常
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    // 获取扫描到的 @PermitAll URL
    Set<String> permitAllUrls = permitAllUrlScanner.getPermitAllUrls();

    // 打印扫描结果
    // log.info("动态放行的 @PermitAll 接口: {}", permitAllUrls);

    http
        // 启用 CORS
        .cors(cors -> cors.configurationSource(corsConfigurationSource))
        // 禁用 CSRF（前后端分离 + JWT 无状态）
        .csrf(AbstractHttpConfigurer::disable)
        // 禁用默认登录页
        .formLogin(AbstractHttpConfigurer::disable)
        // 禁用 HTTP Basic
        .httpBasic(AbstractHttpConfigurer::disable)
        // 无状态会话（不创建 Session）
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // 授权规则
        .authorizeHttpRequests(
            auth -> {
              // 动态放行所有 @PermitAll 的 URL
              permitAllUrls.forEach(url -> auth.requestMatchers(url).permitAll());

              // 静态白名单放行
              auth.requestMatchers("/ws/**")
                  .permitAll()
                  .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                  .permitAll()
                  .requestMatchers(
                      "/doc.html", "/webjars/**", "/swagger-resources/**", "/favicon.ico")
                  .permitAll()
                  // 其他未匹配的请求必须认证
                  .anyRequest()
                  .authenticated();
            })
        // 异常处理：401 未认证，403 无权限
        .exceptionHandling(
            ex ->
                ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                    .accessDeniedHandler(
                        (request, response, accessDeniedException) ->
                            response.setStatus(HttpStatus.FORBIDDEN.value())))
        // 添加 JWT 过滤器
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  /**
   * 静态资源跳过 Security 过滤器链
   *
   * @return WebSecurityCustomizer
   */
  @Bean
  public WebSecurityCustomizer webSecurityCustomizer() {
    return (web) ->
        web.ignoring()
            .requestMatchers(
                "/", "/index.html", "/assets/**", "/*.ico", "/*.png", "/*.svg", "/ws/**");
  }

  /**
   * 密码编码器（兼容明文 + BCrypt）
   *
   * @return PasswordEncoder
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new CompatiblePasswordEncoder();
  }

  /**
   * 认证管理器
   *
   * @param config AuthenticationConfiguration
   * @return AuthenticationManager
   * @throws Exception 配置异常
   */
  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
      throws Exception {
    return config.getAuthenticationManager();
  }
}
