package com.rongia.system.application.security.token;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** 令牌颁发端口 应用层通过此接口完成登录与登出，不关心底层令牌实现（不透明令牌状态在 Redis 登记簿） */
public interface TokenIssuer {

  /**
   * 登录并颁发令牌
   *
   * @param userName 账号
   * @param password 密码
   * @return 颁发结果
   * @throws com.rongia.kernel.application.exception.ApiException 账号密码错误时抛出
   */
  @NonNull IssuedToken issue(@NonNull String userName, @NonNull String password);

  /**
   * 登出（吊销当前会话并删除用户缓存）
   *
   * @param token 令牌
   * @return 登出结果，令牌无效或已被吊销时返回 null
   */
  @Nullable
  SignOutResult revoke(@NonNull String token);
}
