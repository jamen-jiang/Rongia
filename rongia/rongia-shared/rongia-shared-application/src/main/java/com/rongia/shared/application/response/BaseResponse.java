package com.rongia.shared.application.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 响应基类 */
@NullMarked
@Getter
@Setter
public class BaseResponse {

  /** 创建人Id（可空） */
  @Schema(description = "创建人Id")
  private Long createdBy = 0L;

  /** 创建人 */
  @Schema(description = "创建人")
  private String createdByName = "";

  /** 创建时间 */
  @Schema(description = "创建时间")
  private LocalDateTime createdTime = LocalDateTime.now();

  /** 更新人Id（可空） */
  @Schema(description = "更新人Id")
  @Nullable
  private Long updatedBy;

  /** 更新人（可空） */
  @Schema(description = "更新人")
  @Nullable
  private String updatedByName;

  /** 更新时间（可空） */
  @Schema(description = "更新时间")
  @Nullable
  private LocalDateTime updatedTime;
}
