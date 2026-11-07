package com.rongia.shared.infrastructure.security.user;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import org.jspecify.annotations.Nullable;

/**
 * 登录用户工具：读写 TokenSession 里的登录用户快照（对齐 Snowy 的 StpLoginUserUtil）
 *
 * <p>Snowy 模式：登录时把 {@link LoginUser} 整个存进 TokenSession（随令牌存活），
 * 校验/取身份时直接读快照，不再查库、不再有独立缓存层。
 * 代价：改权限后旧会话仍是旧快照，需重新登录刷新。
 */
public final class StpLoginUserUtil {

  /** 登录用户快照在 TokenSession 中的 key */
  public static final String LOGIN_USER_KEY = "loginUser";

  private StpLoginUserUtil() {}

  /**
   * 取当前请求的登录用户快照
   *
   * @return 登录用户，未登录 / 无请求上下文 / 快照缺失时返回 null
   */
  public static @Nullable LoginUser getLoginUser() {
    try {
      SaSession session = StpUtil.getTokenSession();
      Object value = session.get(LOGIN_USER_KEY);
      return value instanceof LoginUser loginUser ? loginUser : null;
    } catch (NotLoginException e) {
      return null;
    }
  }
}
