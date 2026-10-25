package com.rongia.system.domain.operationlog;

import com.rongia.shared.domain.aggregate.LogAggregateRoot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 操作日志 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "OperationLog",
    indexes = {
      @Index(name = "IX_OperationLog_TraceId", columnList = "TraceId"),
      @Index(name = "IX_OperationLog_ApiModule", columnList = "ApiModule"),
      @Index(name = "IX_OperationLog_ApiName", columnList = "ApiName"),
      @Index(name = "IX_OperationLog_ApiUrl", columnList = "ApiUrl")
    })
public class OperationLog extends LogAggregateRoot {

  /** TraceId */
  @Nationalized
  @Column(name = "TraceId", nullable = false, length = 50)
  private String traceId;

  /** 接口模块 */
  @Nationalized
  @Column(name = "ApiModule", nullable = false, length = 50)
  private String apiModule;

  /** 接口名称 */
  @Nationalized
  @Column(name = "ApiName", nullable = false, length = 50)
  private String apiName;

  /** 接口地址 */
  @Nationalized
  @Column(name = "ApiUrl", nullable = false, length = 500)
  private String apiUrl;

  /** 接口提交方法 */
  @Nationalized
  @Column(name = "ApiMethod", nullable = false, length = 50)
  private String apiMethod;

  /** IP地址（可空） */
  @Nationalized
  @Column(name = "IP", length = 200)
  @Nullable
  private String ip;

  /** 客户端地点（可空） */
  @Nationalized
  @Column(name = "City", length = 50)
  @Nullable
  private String city;

  /** 客户端 UserAgent（可空） */
  @Nationalized
  @Column(name = "UserAgent", length = 500)
  @Nullable
  private String userAgent;

  /** 浏览器（可空） */
  @Nationalized
  @Column(name = "Browser", length = 200)
  @Nullable
  private String browser;

  /** 操作系统（可空） */
  @Nationalized
  @Column(name = "Os", length = 200)
  @Nullable
  private String os;

  /**
   * 构造器
   *
   * @param traceId TraceId
   * @param userId 用户Id
   * @param userName 用户名称
   * @param startedOn 开始时间
   * @param endedOn 结束时间
   * @param elapsedMilliseconds 耗时（毫秒）
   * @param apiModule 接口模块
   * @param apiName 接口名称
   * @param apiUrl 接口地址
   * @param apiMethod 接口提交方法
   * @param ip IP地址（可空）
   * @param city 客户端地点（可空）
   * @param userAgent 客户端 UserAgent（可空）
   * @param browser 浏览器（可空）
   * @param os 操作系统（可空）
   * @param request 请求数据（可空）
   * @param response 返回数据（可空）
   * @param isSuccess 是否成功
   * @param errorMessage 错误信息（可空）
   * @param exception 异常信息（可空）
   */
  public OperationLog(
      String traceId,
      @Nullable Long userId,
      @Nullable String userName,
      LocalDateTime startedOn,
      LocalDateTime endedOn,
      long elapsedMilliseconds,
      String apiModule,
      String apiName,
      String apiUrl,
      String apiMethod,
      @Nullable String ip,
      @Nullable String city,
      @Nullable String userAgent,
      @Nullable String browser,
      @Nullable String os,
      @Nullable String request,
      @Nullable String response,
      boolean isSuccess,
      @Nullable String errorMessage,
      @Nullable String exception) {
    super(
        userId,
        userName,
        startedOn,
        endedOn,
        elapsedMilliseconds,
        request,
        response,
        isSuccess,
        errorMessage,
        exception);
    this.traceId = traceId;
    this.apiModule = apiModule;
    this.apiName = apiName;
    this.apiUrl = apiUrl;
    this.apiMethod = apiMethod;
    this.ip = ip;
    this.city = city;
    this.userAgent = userAgent;
    this.browser = browser;
    this.os = os;
  }
}
