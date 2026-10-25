package com.rongia.system.application.signinlog.command;

import com.rongia.shared.application.cqrs.Command;
import com.rongia.shared.application.cqrs.NoLogCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 添加登录日志命令 */
@NullMarked
@Getter
@Schema(description = "添加登录日志命令")
public class AddSignInLogCommand extends Command implements NoLogCommand {

  /** 登录类型 */
  @Schema(description = "登录类型", requiredMode = Schema.RequiredMode.REQUIRED)
  private final String type;

  /** 用户Id（可空） */
  @Schema(description = "用户Id")
  @Nullable
  private final Long userId;

  /** 登录账号（可空） */
  @Schema(description = "登录账号")
  @Nullable
  private final String userCode;

  /** 用户名称（可空） */
  @Schema(description = "用户名称")
  @Nullable
  private final String userName;

  /** 开始时间 */
  @Schema(description = "开始时间", requiredMode = Schema.RequiredMode.REQUIRED)
  private final LocalDateTime startedOn;

  /** 结束时间 */
  @Schema(description = "结束时间", requiredMode = Schema.RequiredMode.REQUIRED)
  private final LocalDateTime endedOn;

  /** 耗时（毫秒） */
  @Schema(description = "耗时（毫秒）", requiredMode = Schema.RequiredMode.REQUIRED)
  private final Long elapsedMilliseconds;

  /** 令牌（可空） */
  @Schema(description = "令牌")
  @Nullable
  private final String token;

  /** 刷新令牌（可空） */
  @Schema(description = "刷新令牌")
  @Nullable
  private final String refreshToken;

  /** 旧刷新令牌（可空） */
  @Schema(description = "旧刷新令牌")
  @Nullable
  private final String oldRefreshToken;

  /** IP地址（可空） */
  @Schema(description = "IP地址")
  @Nullable
  private final String ip;

  /** 客户端地点（可空） */
  @Schema(description = "客户端地点")
  @Nullable
  private final String city;

  /** 客户端 UserAgent（可空） */
  @Schema(description = "客户端 UserAgent")
  @Nullable
  private final String userAgent;

  /** 浏览器（可空） */
  @Schema(description = "浏览器")
  @Nullable
  private final String browser;

  /** 操作系统（可空） */
  @Schema(description = "操作系统")
  @Nullable
  private final String os;

  /** 是否成功 */
  @Schema(description = "是否成功", requiredMode = Schema.RequiredMode.REQUIRED)
  private final Boolean isSuccess;

  /** 错误信息（可空） */
  @Schema(description = "错误信息")
  @Nullable
  private final String errorMessage;

  /** 异常信息（可空） */
  @Schema(description = "异常信息")
  @Nullable
  private final String exception;

  /**
   * 构造器
   *
   * @param type 登录类型
   * @param userId 用户Id（可空）
   * @param userCode 登录账号（可空）
   * @param userName 用户名称（可空）
   * @param startedOn 开始时间
   * @param endedOn 结束时间
   * @param elapsedMilliseconds 耗时（毫秒）
   * @param token 令牌（可空）
   * @param refreshToken 刷新令牌（可空）
   * @param oldRefreshToken 旧刷新令牌（可空）
   * @param ip IP地址（可空）
   * @param city 客户端地点（可空）
   * @param userAgent 客户端 UserAgent（可空）
   * @param browser 浏览器（可空）
   * @param os 操作系统（可空）
   * @param isSuccess 是否成功
   * @param errorMessage 错误信息（可空）
   * @param exception 异常信息（可空）
   */
  public AddSignInLogCommand(
      String type,
      @Nullable Long userId,
      @Nullable String userCode,
      @Nullable String userName,
      LocalDateTime startedOn,
      LocalDateTime endedOn,
      long elapsedMilliseconds,
      @Nullable String token,
      @Nullable String refreshToken,
      @Nullable String oldRefreshToken,
      @Nullable String ip,
      @Nullable String city,
      @Nullable String userAgent,
      @Nullable String browser,
      @Nullable String os,
      @Nullable Boolean isSuccess,
      @Nullable String errorMessage,
      @Nullable String exception) {
    this.type = type;
    this.userId = userId;
    this.userCode = userCode;
    this.userName = userName;
    this.startedOn = startedOn;
    this.endedOn = endedOn;
    this.elapsedMilliseconds = elapsedMilliseconds;
    this.token = token;
    this.refreshToken = refreshToken;
    this.oldRefreshToken = oldRefreshToken;
    this.ip = ip;
    this.city = city;
    this.userAgent = userAgent;
    this.browser = browser;
    this.os = os;
    this.isSuccess = Objects.requireNonNullElse(isSuccess, false);
    this.errorMessage = errorMessage;
    this.exception = exception;
  }
}
