package com.rongia.shared.infrastructure.util;

import jakarta.servlet.http.HttpServletRequest;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/** Web 工具：获取 IP、解析 UserAgent */
public final class WebUtils {

  /** UserAgent 解析器（带缓存） */
  private static final UserAgentAnalyzer USER_AGENT_ANALYZER =
      UserAgentAnalyzer.newBuilder()
          .hideMatcherLoadStats()
          .withCache(10000)
          .withFields("AgentName", "OperatingSystemName", "DeviceClass")
          .build();

  /** 未知值占位 */
  private static final String UNKNOWN = "Unknown";

  /** IPv4 正则 */
  private static final Pattern IPV4_PATTERN =
      Pattern.compile("^((2[0-4]\\d|25[0-5]|[01]?\\d\\d?)\\.){3}(2[0-4]\\d|25[0-5]|[01]?\\d\\d?)$");

  private WebUtils() {}

  /**
   * 获取客户端真实 IP（对标 .NET WebUtils.GetClientIp）。
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
  public static String getClientIp(@Nullable HttpServletRequest request) {
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
  public static String getBrowser(HttpServletRequest request) {
    return resolve(request, "AgentName");
  }

  /**
   * 解析操作系统名称
   *
   * @param request 请求对象
   * @return 操作系统名称，解析不出返回 null
   */
  public static String getOs(HttpServletRequest request) {
    return resolve(request, "OperatingSystemName");
  }

  /**
   * 解析设备类型
   *
   * @param request 请求对象
   * @return 设备类型，解析不出返回 null
   */
  public static String getDeviceClass(HttpServletRequest request) {
    return resolve(request, "DeviceClass");
  }

  /**
   * 从 User-Agent 中解析指定字段
   *
   * @param request 请求对象
   * @param field UserAgentAnalyzer 的字段名
   * @return 解析结果，Unknown 或空 UA 时返回 null
   */
  @Nullable
  private static String resolve(@Nullable HttpServletRequest request, String field) {
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
