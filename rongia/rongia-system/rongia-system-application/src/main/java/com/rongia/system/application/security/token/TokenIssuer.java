package com.rongia.system.application.security.token;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** 令牌颁发端口 应用层通过此接口完成"登录 / 刷新令牌"，不关心底层是 JWT 还是别的实现 */
public interface TokenIssuer {

  /**
   * 登录并颁发令牌
   *
   * @param userName 账号
   * @param password 密码
   * @return 令牌对
   * @throws com.rongia.shared.application.exception.ApiException 账号密码错误时抛出
   */
  @NonNull TokenPair issue(@NonNull String userName, @NonNull String password);

  /**
   * 刷新令牌
   *
   * @param refreshToken 刷新令牌
   * @return 新的令牌对
   * @throws com.rongia.shared.application.exception.ApiException 刷新令牌无效或已被撤销时抛出
   */
  @NonNull TokenPair refresh(@NonNull String refreshToken);

  /**
   * 登出（拉黑当前 access token + 删除 refresh token + 删除用户缓存）
   *
   * @param refreshToken 刷新令牌
   * @return 登出结果，refresh token 无效时返回 null
   */
  @Nullable
  SignOutResult revoke(@NonNull String refreshToken);
}
