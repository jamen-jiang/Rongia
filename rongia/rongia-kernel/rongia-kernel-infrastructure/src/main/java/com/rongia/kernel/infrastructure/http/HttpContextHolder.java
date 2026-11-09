package com.rongia.kernel.infrastructure.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.regex.Pattern;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
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

  /** UserAgent 解析器（带缓存） */
  private static final UserAgentAnalyzer USER_AGENT_ANALYZER =
      UserAgentAnalyzer.newBuilder()
          .hideMatcherLoadStats()
          .withCache(10000)
          .withFields("AgentName", "OperatingSystemName")
          .build();

  /** UserAgent 解析的未知值占位 */
  private static final String UNKNOWN = "Unknown";

  /** IPv4 正则 */
  private static final Pattern IPV4_PATTERN =
      Pattern.compile("^((2[0-4]\\d|25[0-5]|[01]?\\d\\d?)\\.){3}(2[0-4]\\d|25[0-5]|[01]?\\d\\d?)$");

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
    String ip = getClientIp(request);
    String userAgent = request.getHeader("User-Agent");
    String browser = getBrowser(request);
    String os = getOs(request);

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
   * 解析 Authorization 头中的令牌（去掉 Bearer 前缀）
   *
   * @param request 请求对象
   * @return 令牌，不存在返回 null
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

  // ==================== 客户端信息解析 ====================

  /**
   * 获取客户端真实 IP
   *
   * <p>规则：
   *
   * <ol>
   *   <li>取 X-Forwarded-For 第一个
   *   <li>没有就取 RemoteAddr
   *   <li>空或 ::1 → 127.0.0.1
   *   <li>::ffff: 前缀替换成 127.0.0.1
   *   <li>IPv4 正则校验，不通过 → 127.0.0.1
   * </ol>
   *
   * @param request 请求对象
   * @return 客户端 IP，非 IPv4 时统一返回 127.0.0.1
   */
  private static String getClientIp(@Nullable HttpServletRequest request) {
    if (request == null) {
      return "";
    }

    // 1. 取 X-Forwarded-For 第一个
    String result = request.getHeader("X-Forwarded-For");
    if (StringUtils.isNotBlank(result)) {
      result = result.split(",")[0].trim();
    }

    // 2. 没有就取 RemoteAddr
    if (StringUtils.isBlank(result)) {
      result = request.getRemoteAddr();
    }

    // 3. 空或 ::1 → 127.0.0.1
    if (StringUtils.isBlank(result) || result.contains("::1")) {
      result = "127.0.0.1";
    }

    // 4. ::ffff: 前缀替换
    result = result.replace("::ffff:", "127.0.0.1");

    // 5. IPv4 校验，不通过 → 127.0.0.1
    if (!IPV4_PATTERN.matcher(result).matches()) {
      result = "127.0.0.1";
    }
    return result;
  }

  /**
   * 解析浏览器名称
   *
   * @param request 请求对象
   * @return 浏览器名称，解析不出返回 null
   */
  @Nullable
  private static String getBrowser(@Nullable HttpServletRequest request) {
    return resolveUserAgent(request, "AgentName");
  }

  /**
   * 解析操作系统名称
   *
   * @param request 请求对象
   * @return 操作系统名称，解析不出返回 null
   */
  @Nullable
  private static String getOs(@Nullable HttpServletRequest request) {
    return resolveUserAgent(request, "OperatingSystemName");
  }

  /**
   * 从 User-Agent 中解析指定字段
   *
   * @param request 请求对象
   * @param field UserAgentAnalyzer 的字段名
   * @return 解析结果，Unknown 或空 UA 时返回 null
   */
  @Nullable
  private static String resolveUserAgent(@Nullable HttpServletRequest request, String field) {
    if (request == null) {
      return null;
    }
    String uaString = request.getHeader("User-Agent");
    if (StringUtils.isBlank(uaString)) {
      return null;
    }
    String value = USER_AGENT_ANALYZER.parse(uaString).getValue(field);
    return UNKNOWN.equals(value) ? null : value;
  }
}
