package com.rongia.kernel.infrastructure.security;

import org.jspecify.annotations.Nullable;

/**
 * 后台任务的登录用户上下文
 *
 * <p>JobRunr 执行线程没有 HTTP 请求上下文，Sa-Token 取不到身份；
 * JobContextFilter 在任务执行前把 LoginUser 塞进来，任务结束时清理。
 */
public final class JobContextHolder {

  /** 线程级登录用户 */
  private static final ThreadLocal<LoginUser> CONTEXT = new ThreadLocal<>();

  private JobContextHolder() {}

  /**
   * 绑定登录用户到当前线程
   *
   * @param loginUser 登录用户
   */
  public static void set(LoginUser loginUser) {
    CONTEXT.set(loginUser);
  }

  /**
   * 取当前线程的登录用户
   *
   * @return 登录用户，未绑定返回 null
   */
  public static @Nullable LoginUser get() {
    return CONTEXT.get();
  }

  /** 清理当前线程的登录用户（线程池复用，必须清） */
  public static void clear() {
    CONTEXT.remove();
  }
}
