package com.rongia.system.application.commandlog.command;

import com.rongia.shared.application.cqrs.Command;
import com.rongia.shared.application.cqrs.NoLogCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 添加命令日志命令
 *
 * <p>实现 {@link NoLogCommand}，使 {@code CommandLoggingInterceptor} 跳过它自身， 避免「记录日志的命令又被记录」造成无限递归。
 */
@NullMarked
@Getter
@Schema(description = "添加命令日志命令")
public class AddCommandLogCommand extends Command implements NoLogCommand {

  /** 追踪Id（可空） */
  @Schema(description = "追踪Id")
  @Nullable
  private final String traceId;

  /** 用户Id（可空，0 视为 null） */
  @Schema(description = "用户Id")
  @Nullable
  private final Long userId;

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

  /** 命令名称 */
  @Schema(description = "命令名称", requiredMode = Schema.RequiredMode.REQUIRED)
  private final String name;

  /** 命令类型名称 */
  @Schema(description = "命令类型名称", requiredMode = Schema.RequiredMode.REQUIRED)
  private final String typeName;

  /** 请求数据（可空） */
  @Schema(description = "请求数据")
  @Nullable
  private final String request;

  /** 返回数据（可空） */
  @Schema(description = "返回数据")
  @Nullable
  private final String response;

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
   * @param traceId 追踪Id（可空）
   * @param userId 用户Id（可空，0 视为 null）
   * @param userName 用户名称（可空）
   * @param startedOn 开始时间
   * @param endedOn 结束时间
   * @param elapsedMilliseconds 耗时（毫秒）
   * @param name 命令名称
   * @param typeName 命令类型名称
   * @param request 请求数据（可空）
   * @param response 返回数据（可空）
   * @param isSuccess 是否成功（可空，null 视为 false）
   * @param errorMessage 错误信息（可空）
   * @param exception 异常信息（可空）
   */
  public AddCommandLogCommand(
      @Nullable String traceId,
      @Nullable Long userId,
      @Nullable String userName,
      LocalDateTime startedOn,
      LocalDateTime endedOn,
      long elapsedMilliseconds,
      String name,
      String typeName,
      @Nullable String request,
      @Nullable String response,
      @Nullable Boolean isSuccess,
      @Nullable String errorMessage,
      @Nullable String exception) {
    this.traceId = traceId;
    // userId 为 0 时视为未登录，连同 userName 一起置空
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
    this.name = name;
    this.typeName = typeName;
    this.request = request;
    this.response = response;
    this.isSuccess = Objects.requireNonNullElse(isSuccess, false);
    this.errorMessage = errorMessage;
    this.exception = exception;
  }
}
