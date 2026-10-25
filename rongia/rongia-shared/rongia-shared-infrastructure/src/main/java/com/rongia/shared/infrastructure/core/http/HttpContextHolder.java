package com.rongia.shared.infrastructure.core.http;

import com.rongia.shared.infrastructure.util.WebUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

/** HTTP 上下文工具（对标 .NET CurrentHttpContextAccessor） */
public final class HttpContextHolder {

  /** Authorization 前缀 */
  private static final String BEARER_PREFIX = "Bearer ";

  /** 线程内上下文（异步任务恢复用） */
  private static final ThreadLocal<HttpContext> CURRENT = new ThreadLocal<>();

  private HttpContextHolder() {}

  /**
   * 获取当前 HTTP 上下文
   *
   * <p>优先取 ThreadLocal；没有则从当前请求构建
   *
   * @return 当前上下文，无请求时返回 null
   */
  @Nullable
  public static HttpContext getContext() {
    HttpContext httpContext = CURRENT.get();
    if (httpContext != null) {
      return httpContext;
    }
    ServletRequestAttributes attrs =
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    return attrs == null ? null : build(attrs.getRequest());
  }

  /**
   * 直接设置上下文（不返回句柄，清理由调用方负责）
   *
   * @param context 上下文
   */
  public static void set(HttpContext context) {
    CURRENT.set(context);
  }

  //  /**
  //   * 替换上下文，关闭时恢复父级（配合 try-with-resources）
  //   *
  //   * @param context 新的上下文
  //   * @return 关闭句柄，关闭时恢复父级上下文
  //   */
  //  public static AutoCloseable change(HttpContext context) {
  //    HttpContext parent = CURRENT.get();
  //    CURRENT.set(context);
  //    return () -> CURRENT.set(parent);
  //  }

  /** 清理当前线程的上下文 */
  public static void clear() {
    CURRENT.remove();
  }

  // ==================== 构建 ====================

  /**
   * 从请求构建 HTTP 上下文
   *
   * @param request 请求对象
   * @return 上下文
   */
  private static @NonNull HttpContext build(HttpServletRequest request) {
    String token = resolveToken(request);
    String traceId = resolveTraceId(request);
    String apiUrl = request.getRequestURI();
    String apiMethod = request.getMethod().toLowerCase();
    String ip = WebUtils.getClientIp(request);
    String userAgent = request.getHeader("User-Agent");
    String browser = WebUtils.getBrowser(request);
    String os = WebUtils.getOs(request);

    String apiModule = null;
    String apiName = null;
    Object handler = request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
    if (handler instanceof HandlerMethod hm) {
      apiModule = resolveModule(hm);
      apiName = resolveName(hm);
    }

    return new HttpContext(
        token, traceId, apiModule, apiName, apiUrl, apiMethod, ip, userAgent, browser, os);
  }

  /**
   * 解析 Authorization 头中的 accessToken（去掉 Bearer 前缀）
   *
   * @param request 请求对象
   * @return accessToken，不存在返回 null
   */
  @Nullable
  private static String resolveToken(HttpServletRequest request) {
    String authorization = request.getHeader("Authorization");
    if (StringUtils.isBlank(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
      return null;
    }
    return authorization.substring(BEARER_PREFIX.length());
  }

  /**
   * 解析模块名：@Tag.name() → 类名
   *
   * @param hm 处理方法
   * @return 模块名
   */
  private static String resolveModule(HandlerMethod hm) {
    Tag tag = hm.getBeanType().getAnnotation(Tag.class);
    return (tag != null && StringUtils.isNotBlank(tag.name()))
        ? tag.name()
        : hm.getBeanType().getSimpleName();
  }

  /**
   * 解析接口名：@Operation.summary() → 方法名
   *
   * @param hm 处理方法
   * @return 接口名
   */
  private static String resolveName(HandlerMethod hm) {
    Operation op = hm.getMethodAnnotation(Operation.class);
    return (op != null && StringUtils.isNotBlank(op.summary()))
        ? op.summary()
        : hm.getMethod().getName();
  }

  /**
   * 解析 traceId：优先取 request 属性，没有则用 requestId
   *
   * @param request 请求对象
   * @return traceId
   */
  private static String resolveTraceId(HttpServletRequest request) {
    String traceId = (String) request.getAttribute("traceId");
    return traceId != null ? traceId : request.getRequestId();
  }
}
