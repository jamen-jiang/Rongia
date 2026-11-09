package com.rongia.system.infrastructure.security.token;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.rongia.shared.application.exception.ApiException;
import com.rongia.shared.infrastructure.security.user.LoginUser;
import com.rongia.shared.infrastructure.security.user.StpLoginUserUtil;
import com.rongia.system.application.security.token.IssuedToken;
import com.rongia.system.application.security.token.SignOutResult;
import com.rongia.system.application.security.token.TokenIssuer;
import com.rongia.system.infrastructure.security.user.LoginUserService;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 不透明令牌颁发器：登录 / 登出统一实现（Snowy 模式）
 *
 * <p>令牌与登录态全权交给 Sa-Token（Redis 存储，校验自动续期）；
 * 本类只负责验密、登录登记（TokenSession 写身份快照）与登出。
 */
@Slf4j
@Component
public class OpaqueTokenIssuer implements TokenIssuer {

  /** 密码编码器（BCrypt） */
  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  /** 登录用户装配服务 */
  private final LoginUserService loginUserService;

  /**
   * 构造器注入
   *
   * @param loginUserService 登录用户装配服务
   */
  public OpaqueTokenIssuer(LoginUserService loginUserService) {
    this.loginUserService = loginUserService;
  }

  /**
   * 登录并颁发令牌
   *
   * @param userName 账号
   * @param password 密码
   * @return 颁发结果
   */
  @Override
  public @NonNull IssuedToken issue(@NonNull String userName, @NonNull String password) {
    // 1. 查用户并验密（用户不存在与密码错误统一文案，防账号枚举）
    LoginUser loginUser;
    try {
      loginUser = loginUserService.loadUserByUsername(userName);
    } catch (ApiException e) {
      throw new ApiException("账号或密码错误");
    }
    if (!passwordEncoder.matches(password, loginUser.getPassword())) {
      throw new ApiException("账号或密码错误");
    }

    // 2. Sa-Token 登记登录态（Redis，校验自动续期），TokenSession 写身份快照
    StpUtil.login(loginUser.getUserId());
    StpUtil.getTokenSession().set(StpLoginUserUtil.LOGIN_USER_KEY, loginUser);

    String token = StpUtil.getTokenValue();
    log.info("用户登录成功: {} (userId={})", loginUser.getUserName(), loginUser.getUserId());
    return new IssuedToken(
        loginUser.getUserId(), loginUser.getUserName(), loginUser.getName(), token);
  }

  /**
   * 登出：按令牌注销当前会话
   *
   * @param token 令牌
   * @return 登出结果，令牌无效或已被吊销时返回 null
   */
  @Override
  public @Nullable SignOutResult revoke(@NonNull String token) {
    // 1. 查令牌→账号映射：null = 不存在或已过期；负数哨兵 = 已注销/被踢/被顶
    Object loginId = StpUtil.getLoginIdByToken(token);
    if (loginId == null || (loginId instanceof Number number && number.longValue() < 0)) {
      return null;
    }

    // 2. 注销前先取快照（供日志展示）
    SaSession session = StpUtil.stpLogic.getTokenSessionByToken(token, false);
    Object value = session == null ? null : session.get(StpLoginUserUtil.LOGIN_USER_KEY);
    LoginUser loginUser = value instanceof LoginUser user ? user : null;

    // 3. 按令牌注销（删映射与 TokenSession）
    StpUtil.logoutByTokenValue(token);

    log.info("用户登出: {} (userId={})", loginId, loginId);
    if (loginUser == null) {
      return new SignOutResult(Long.valueOf(loginId.toString()), null, null);
    }
    return new SignOutResult(loginUser.getUserId(), loginUser.getUserName(), loginUser.getName());
  }
}
