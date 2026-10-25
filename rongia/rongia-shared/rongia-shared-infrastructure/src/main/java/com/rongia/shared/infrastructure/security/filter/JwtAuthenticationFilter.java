package com.rongia.shared.infrastructure.security.filter;

import com.rongia.shared.infrastructure.security.token.JwtProvider;
import com.rongia.shared.infrastructure.security.token.TokenService;
import com.rongia.shared.infrastructure.security.user.LoginUser;
import com.rongia.shared.infrastructure.security.user.LoginUserCache;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j; // ★ 引入 Lombok 日志
import org.jspecify.annotations.NullMarked;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** JWT 认证过滤器 */
@Slf4j // ★ 加上日志注解
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  /** JWT 工具类 */
  private final JwtProvider jwtProvider;

  /** 用户详情服务 */
  private final UserDetailsService userDetailsService;

  /** 登录用户缓存 */
  private final LoginUserCache loginUserCache;

  /** 令牌 Redis 管理 */
  private final TokenService tokenService;

  /**
   * 构造器注入
   *
   * @param jwtProvider JWT 工具类
   * @param userDetailsService 用户详情服务
   * @param loginUserCache 登录用户缓存
   * @param tokenService 令牌 Redis 管理
   */
  public JwtAuthenticationFilter(
          JwtProvider jwtProvider,
          UserDetailsService userDetailsService,
          LoginUserCache loginUserCache,
          TokenService tokenService) {
    this.jwtProvider = jwtProvider;
    this.userDetailsService = userDetailsService;
    this.loginUserCache = loginUserCache;
    this.tokenService = tokenService;
  }

  /**
   * 过滤请求
   *
   * @param request 请求
   * @param response 响应
   * @param filterChain 过滤器链
   * @throws ServletException Servlet 异常
   * @throws IOException IO 异常
   */
  @Override
  @NullMarked
  protected void doFilterInternal(
          HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
          throws ServletException, IOException {
    // 1. 从 Authorization 头取 token
    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      // 没带 Token，可能是匿名接口，直接放行给 Security 判断
      filterChain.doFilter(request, response);
      return;
    }

    String token = authHeader.substring(7);

    // 2. 验证 token 有效性（签名 + 过期时间）
    if (!jwtProvider.isValidToken(token)) {
      log.debug("无效的 JWT Token，放行交由 Security 处理"); // ★ 增加日志
      filterChain.doFilter(request, response);
      return;
    }

    // 3. 检查黑名单（登出 / 踢人下线的 token 会在这里被拦截）
    String jti = jwtProvider.getJti(token);
    if (tokenService.isBlacklisted(jti)) {
      log.debug("JWT Token 已被列入黑名单，放行交由 Security 处理"); // ★ 增加日志
      filterChain.doFilter(request, response);
      return;
    }

    // 4. 从 token 取 userId
    Long userId = jwtProvider.getUserId(token);

    // 5. 未认证时，塞入 SecurityContext
    if (SecurityContextHolder.getContext().getAuthentication() == null) {
      // 5.1 从 Redis 缓存拿 LoginUser
      LoginUser loginUser = loginUserCache.get(userId);

      // 5.2 缓存未命中，查数据库；查不到就当无效 token 处理
      if (loginUser == null) {
        String userName = jwtProvider.getUserName(token);
        try {
          loginUser = (LoginUser) userDetailsService.loadUserByUsername(userName);
        } catch (UsernameNotFoundException e) {
          log.warn("JWT Token 中的用户 {} 在数据库中不存在，放行交由 Security 处理", userName); // ★ 增加日志
          filterChain.doFilter(request, response);
          return;
        }
        // 5.3 写回缓存
        loginUserCache.put(loginUser);
      }

      // 5.4 塞进 SecurityContext
      UsernamePasswordAuthenticationToken authentication =
              new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
      authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
      SecurityContextHolder.getContext().setAuthentication(authentication);

      log.debug("JWT 认证成功，用户: {}", loginUser.getUsername()); // ★ 增加日志
    }

    filterChain.doFilter(request, response);
  }
}