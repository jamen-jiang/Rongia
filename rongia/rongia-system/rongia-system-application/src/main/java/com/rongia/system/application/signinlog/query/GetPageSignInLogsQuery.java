package com.rongia.system.application.signinlog.query;

import com.rongia.shared.application.cqrs.Query;
import com.rongia.shared.application.request.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 分页查询登录日志列表 */
@NullMarked
@Getter
@Setter
@Schema(description = "分页查询登录日志列表")
public class GetPageSignInLogsQuery extends PageRequest implements Query {

  /** 类型（可空） */
  @Schema(description = "类型")
  @Nullable
  private String type;

  /** 用户账号（可空） */
  @Schema(description = "用户账号")
  @Nullable
  private String userCode;

  /** 用户名称（可空） */
  @Schema(description = "用户名称")
  @Nullable
  private String userName;

  /** 开始时间（可空） */
  @Schema(description = "开始时间")
  @Nullable
  private LocalDateTime startedOn;

  /** 结束时间（可空） */
  @Schema(description = "结束时间")
  @Nullable
  private LocalDateTime endedOn;

  /** 是否成功（可空） */
  @Schema(description = "是否成功")
  @Nullable
  private Boolean isSuccess;
}
