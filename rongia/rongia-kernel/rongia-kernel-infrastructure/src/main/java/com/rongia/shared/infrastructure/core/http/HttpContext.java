package com.rongia.shared.infrastructure.core.http;

import lombok.Getter;
import lombok.Setter;

/** 当前 HTTP 上下文 */
@Getter
@Setter
public class HttpContext {

  /** 令牌（Bearer 后面的部分） */
  private String token;

  /** 追踪Id */
  private String traceId;

  /** 模块名 */
  private String apiModule;

  /** 接口名 */
  private String apiName;

  /** 请求地址 */
  private String apiUrl;

  /** 请求方法（小写） */
  private String apiMethod;

  /** 客户端 IP */
  private String ip;

  /** User-Agent */
  private String userAgent;

  /** 浏览器 */
  private String browser;

  /** 操作系统 */
  private String os;

  /** 无参构造（Jackson 反序列化需要） */
  public HttpContext() {}

  /**
   * 全参构造
   *
   * @param token 令牌（Bearer 后面的部分，可空）
   * @param traceId 追踪Id
   * @param apiModule 模块名
   * @param apiName 接口名
   * @param apiUrl 请求地址
   * @param apiMethod 请求方法（小写）
   * @param ip 客户端 IP
   * @param userAgent User-Agent
   * @param browser 浏览器
   * @param os 操作系统
   */
  public HttpContext(
      String token,
      String traceId,
      String apiModule,
      String apiName,
      String apiUrl,
      String apiMethod,
      String ip,
      String userAgent,
      String browser,
      String os) {
    this.token = token;
    this.traceId = traceId;
    this.apiModule = apiModule;
    this.apiName = apiName;
    this.apiUrl = apiUrl;
    this.apiMethod = apiMethod;
    this.ip = ip;
    this.userAgent = userAgent;
    this.browser = browser;
    this.os = os;
  }
}
