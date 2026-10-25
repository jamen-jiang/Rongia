package com.rongia.system.application.signinlog.query;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 登录日志响应 */
@NullMarked
@Getter
@Setter
@Schema(description = "登录日志响应")
public class SignInLogResponse {

  /** 登录日志Id */
  @Schema(description = "登录日志Id", requiredMode = Schema.RequiredMode.REQUIRED)
  private Long id;

  /** 类型 */
  @Schema(description = "类型", requiredMode = Schema.RequiredMode.REQUIRED)
  private String type = "";

  /** 用户Id（可空） */
  @Schema(description = "用户Id")
  @Nullable
  private Long userId;

  /** 用户账号（可空） */
  @Schema(description = "用户账号")
  @Nullable
  private String userCode;

  /** 用户名称（可空） */
  @Schema(description = "用户名称")
  @Nullable
  private String userName;

  /** 开始时间 */
  @Schema(description = "开始时间", requiredMode = Schema.RequiredMode.REQUIRED)
  private LocalDateTime startedOn;

  /** 结束时间 */
  @Schema(description = "结束时间", requiredMode = Schema.RequiredMode.REQUIRED)
  private LocalDateTime endedOn;

  /** 耗时（毫秒） */
  @Schema(description = "耗时（毫秒）", requiredMode = Schema.RequiredMode.REQUIRED)
  private Long elapsedMilliseconds;

  /** IP地址（可空） */
  @Schema(description = "IP地址")
  @Nullable
  private String ip;

  /** 客户端地点（可空） */
  @Schema(description = "客户端地点")
  @Nullable
  private String city;

  /** 客户端 UserAgent（可空） */
  @Schema(description = "客户端 UserAgent")
  @Nullable
  private String userAgent;

  /** 浏览器（可空） */
  @Schema(description = "浏览器")
  @Nullable
  private String browser;

  /** 操作系统（可空） */
  @Schema(description = "操作系统")
  @Nullable
  private String os;

  /** 是否成功 */
  @Schema(description = "是否成功", requiredMode = Schema.RequiredMode.REQUIRED)
  private Boolean isSuccess;

  /** 错误信息（可空） */
  @Schema(description = "错误信息")
  @Nullable
  private String errorMessage;

  /** 异常信息（可空） */
  @Schema(description = "异常信息")
  @Nullable
  private String exception;
}
