package com.rongia.shared.domain.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;
import org.hibernate.annotations.Nationalized;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 日志聚合根基类 */
@NullMarked
@Getter
@MappedSuperclass
public abstract class LogAggregateRoot extends DeleteAggregateRoot {

  /** 用户Id（可空） */
  @Column(name = "UserId")
  @Nullable
  private Long userId;

  /** 用户名称（可空） */
  @Nationalized
  @Column(name = "UserName", length = 50)
  @Nullable
  private String userName;

  /** 开始时间 */
  @Column(name = "StartedOn", nullable = false)
  private LocalDateTime startedOn;

  /** 结束时间 */
  @Column(name = "EndedOn", nullable = false)
  private LocalDateTime endedOn;

  /** 耗时（毫秒） */
  @Column(name = "ElapsedMilliseconds", nullable = false)
  private Long elapsedMilliseconds;

  /** 请求数据（可空） */
  @Nationalized
  @Column(name = "Request")
  @Nullable
  private String request;

  /** 返回数据（可空） */
  @Nationalized
  @Column(name = "Response")
  @Nullable
  private String response;

  /** 是否成功 */
  @Column(name = "IsSuccess", nullable = false)
  private Boolean isSuccess;

/** 错误信息（可空） */
  @Nationalized
  @Column(name = "ErrorMessage")
  @Nullable
  private String errorMessage;

  /** 异常信息（可空） */
  @Nationalized
  @Column(name = "Exception")
  @Nullable
  private String exception;

  /** 无参构造器（供框架反射使用） */
  protected LogAggregateRoot() {}

  /**
   * 构造器
   *
   * @param userId 用户Id（可空，0 视为 null）
   * @param userName 用户名称（可空）
   * @param startedOn 开始时间
   * @param endedOn 结束时间
   * @param elapsedMilliseconds 耗时（毫秒）
   * @param request 请求数据（可空）
   * @param response 返回数据（可空）
   * @param isSuccess 是否成功
   * @param errorMessage 错误信息（可空）
   * @param exception 异常信息（可空）
   */
  protected LogAggregateRoot(
      @Nullable Long userId,
      @Nullable String userName,
      LocalDateTime startedOn,
      LocalDateTime endedOn,
      long elapsedMilliseconds,
      @Nullable String request,
      @Nullable String response,
      boolean isSuccess,
      @Nullable String errorMessage,
      @Nullable String exception) {
    // userId 为 0 时视为 null
    if (userId != null && userId == 0L) {
      this.userId = null;
      this.userName = null;
    } else {
      this.userId = userId;
      this.userName = userName;
    }
    this.startedOn = startedOn;
    this.endedOn = endedOn;
    this.elapsedMilliseconds = elapsedMilliseconds;
    this.request = request;
    this.response = response;
    this.isSuccess = isSuccess;
    this.errorMessage = errorMessage;
    this.exception = exception;
  }
}
