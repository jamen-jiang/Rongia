package com.rongia.system.domain.commandlog;

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

/**
 * 命令日志
 *
 * <p>由 {@code CommandLoggingInterceptor} 统一写入，记录每个命令的命令名、耗时、请求/响应与异常， 用于审计与问题回溯。与
 * {@code SignInLog} 同属支撑域的技术审计日志。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "CommandLog",
    indexes = {
      @Index(name = "IX_CommandLog_TraceId", columnList = "TraceId"),
      @Index(name = "IX_CommandLog_Name", columnList = "Name"),
      @Index(name = "IX_CommandLog_TypeName", columnList = "TypeName")
    })
public class CommandLog extends LogAggregateRoot {

  /** 追踪Id（可空） */
  @Nationalized
  @Column(name = "TraceId", length = 50)
  @Nullable
  private String traceId;

  /** 命令名称 */
  @Nationalized
  @Column(name = "Name", nullable = false, length = 200)
  private String name;

  /** 命令类型名称 */
  @Nationalized
  @Column(name = "TypeName", nullable = false, length = 200)
  private String typeName;

  /**
   * 构造器
   *
   * @param traceId 追踪Id（可空）
   * @param userId 用户Id（可空）
   * @param userName 用户名称（可空）
   * @param startedOn 开始时间
   * @param endedOn 结束时间
   * @param elapsedMilliseconds 耗时（毫秒）
   * @param name 命令名称
   * @param typeName 命令类型名称
   * @param request 请求数据（可空）
   * @param response 返回数据（可空）
   * @param isSuccess 是否成功
   * @param errorMessage 错误信息（可空）
   * @param exception 异常信息（可空）
   */
  public CommandLog(
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
    this.name = name;
    this.typeName = typeName;
  }
}
