package com.rongia.system.infrastructure.security.token;

import com.rongia.shared.application.enums.ApiStatusEnum;
import com.rongia.shared.application.exception.ApiException;
import com.rongia.shared.infrastructure.core.http.HttpContext;
import com.rongia.shared.infrastructure.core.http.HttpContextHolder;
import com.rongia.shared.infrastructure.security.token.JwtProvider;
import com.rongia.shared.infrastructure.security.token.TokenService;
import com.rongia.shared.infrastructure.security.user.LoginUser;
import com.rongia.shared.infrastructure.security.user.LoginUserCache;
import com.rongia.system.application.security.token.SignOutResult;
import com.rongia.system.application.security.token.TokenIssuer;
import com.rongia.system.application.security.token.TokenPair;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

/** 令牌颁发实现（JWT + Redis） */
@Component
public class JwtTokenIssuer implements TokenIssuer {

  /** 认证管理器 */
  private final AuthenticationManager authenticationManager;

  /** JWT 工具类 */
  private final JwtProvider jwtProvider;

  /** 令牌 Redis 管理 */
  private final TokenService tokenService;

  /** 登录用户缓存 */
  private final LoginUserCache loginUserCache;

  /** 用户详情服务 */
  private final UserDetailsService userDetailsService;

  /**
   * 构造器注入
   *
   * @param authenticationManager 认证管理器
   * @param jwtProvider JWT 工具类
   * @param tokenService 令牌 Redis 管理
   * @param loginUserCache 登录用户缓存
   * @param userDetailsService 用户详情服务
   */
  public JwtTokenIssuer(
      AuthenticationManager authenticationManager,
      JwtProvider jwtProvider,
      TokenService tokenService,
      LoginUserCache loginUserCache,
      UserDetailsService userDetailsService) {
    this.authenticationManager = authenticationManager;
    this.jwtProvider = jwtProvider;
    this.tokenService = tokenService;
    this.loginUserCache = loginUserCache;
    this.userDetailsService = userDetailsService;
  }

  /**
   * 登录并颁发令牌
   *
   * @param userName 账号
   * @param password 密码
   * @return 令牌对
   * @throws ApiException 账号或密码错误时抛出
   */
  @Override
  public @NonNull TokenPair issue(@NonNull String userName, @NonNull String password) {
    // 1. 认证；失败统一返回"账号或密码错误"，不暴露具体原因（防账号枚举）
    Authentication authentication;
    try {
      authentication =
          authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(userName, password));
    } catch (AuthenticationException e) {
      throw new ApiException("账号或密码错误");
    }

    // 2. 认证主体必须是 LoginUser
    Object principal = authentication.getPrincipal();
    if (!(principal instanceof LoginUser loginUser)) {
      throw new ApiException("认证主体类型异常");
    }

    // 3. 缓存登录用户（后续请求从缓存拿权限）
    loginUserCache.put(loginUser);

    // 4. 生成会话标识，同一次登录的 access / refresh token 共用一个会话
    String sessionId = UUID.randomUUID().toString();

    // 5. 生成 access token（短效）+ refresh token（长效）
    String accessToken =
        jwtProvider.generateAccessToken(loginUser.getUserId(), loginUser.getUsername(), sessionId);
    String refreshToken =
        jwtProvider.generateRefreshToken(loginUser.getUserId(), loginUser.getUsername(), sessionId);

    // 6. 存刷新令牌到 Redis（按会话隔离，多端互不影响）
    tokenService.storeRefreshToken(
        loginUser.getUserId(), sessionId, refreshToken, jwtProvider.getRefreshTokenExpiration());

    // 7. 登记本次登录：记录 access token 的 jti（即会话标识）+ 加入在线用户列表
    //    供"踢人下线"和"在线用户列表"使用。存活时长取刷新令牌有效期，与刷新令牌对齐
    tokenService.registerLogin(
        loginUser.getUserId(),
        loginUser.getUsername(),
        jwtProvider.getJti(accessToken),
        getClientIp(),
        jwtProvider.getRefreshTokenExpiration());

    // 8. 返回
    return new TokenPair(
        loginUser.getUserId(),
        loginUser.getUsername(),
        loginUser.getName(),
        accessToken,
        refreshToken);
  }

  /**
   * 刷新令牌
   *
   * @param refreshToken 刷新令牌
   * @return 新的令牌对
   * @throws ApiException 刷新令牌无效或已被撤销时抛出
   */
  @Override
  public @NonNull TokenPair refresh(@NonNull String refreshToken) {
    // 1. 校验刷新令牌签名与有效期
    if (!jwtProvider.isValidToken(refreshToken)) {
      throw new ApiException(ApiStatusEnum.FAIL_TOKEN_UNVALID, "刷新令牌无效或已过期");
    }
    Long userId = jwtProvider.getUserId(refreshToken);

    // 2. 取会话标识
    String sessionId = jwtProvider.getSessionId(refreshToken);
    if (sessionId == null) {
      throw new ApiException(ApiStatusEnum.FAIL_TOKEN_UNVALID, "刷新令牌无效");
    }

    // 3. 校验 Redis 里的刷新令牌是否一致（防旧令牌重放：刷新后会轮换覆盖）
    String stored = tokenService.getRefreshToken(userId, sessionId);
    if (stored == null || !stored.equals(refreshToken)) {
      throw new ApiException(ApiStatusEnum.FAIL_TOKEN_UNVALID, "刷新令牌已被撤销");
    }

    // 4. 从缓存拿 LoginUser；缓存 miss 则查库（顺便校验用户是否还存在）
    LoginUser loginUser = loginUserCache.get(userId);
    if (loginUser == null) {
      String userName = jwtProvider.getUserName(refreshToken);
      Object principal = userDetailsService.loadUserByUsername(userName);
      if (!(principal instanceof LoginUser lu)) {
        throw new ApiException("用户信息异常");
      }
      loginUser = lu;
      loginUserCache.put(loginUser);
    }

    // 5. 生成新 access token + 新 refresh token（轮换：旧 refresh token 立即失效）
    String newAccessToken =
        jwtProvider.generateAccessToken(userId, loginUser.getUsername(), sessionId);
    String newRefreshToken =
        jwtProvider.generateRefreshToken(userId, loginUser.getUsername(), sessionId);

    // 6. 写入新会话的刷新令牌（按会话隔离，不影响该用户其他端）
    tokenService.storeRefreshToken(
        userId, sessionId, newRefreshToken, jwtProvider.getRefreshTokenExpiration());

    // 7. 会话存活时长对齐刷新令牌有效期，避免会话提前从在线列表消失
    tokenService.extendSession(userId, sessionId, jwtProvider.getRefreshTokenExpiration());

    // 8. 返回
    return new TokenPair(
        loginUser.getUserId(),
        loginUser.getUsername(),
        loginUser.getName(),
        newAccessToken,
        newRefreshToken);
  }

  /**
   * 登出
   *
   * @param refreshToken 刷新令牌
   * @return 登出结果，refresh token 无效时返回 null
   */
  @Override
  @Nullable
  public SignOutResult revoke(@NonNull String refreshToken) {
    // 1. 校验刷新令牌；无效直接返回 null
    if (!jwtProvider.isValidToken(refreshToken)) {
      return null;
    }
    Long userId = jwtProvider.getUserId(refreshToken);
    String sessionId = jwtProvider.getSessionId(refreshToken);
    if (sessionId == null) {
      return null;
    }

    // 2. 取 LoginUser（用于日志返回），必须在删缓存之前
    LoginUser loginUser = loginUserCache.get(userId);

    // 3. 拉黑当前 accessToken + 从会话集合移除
    tokenService.revokeCurrentAccessToken();

    // 4. 显式注销本会话：登出接口不保证请求头带 accessToken，此时上面的调用会提前返回，
    //    需按 refresh token 里的会话标识兜底，避免会话残留在在线列表
    tokenService.unregisterLogin(userId, sessionId);

    // 5. 只删除本会话的 refresh token，其他端保持在线
    tokenService.removeRefreshToken(userId, sessionId);

    // 6. 删除 LoginUser 缓存
    loginUserCache.remove(userId);

    // 7. 返回
    if (loginUser != null) {
      return new SignOutResult(loginUser.getUserId(), loginUser.getUsername(), loginUser.getName());
    }
    return new SignOutResult(userId, null, null);
  }

  /**
   * 从 HttpContext 取客户端 IP，取不到返回 null
   *
   * @return 客户端 IP
   */
  @Nullable
  private String getClientIp() {
    HttpContext httpContext = HttpContextHolder.getContext();
    if (httpContext == null) {
      return null;
    }
    return httpContext.getIp();
  }
}
