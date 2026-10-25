package com.rongia.shared.application.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;

/** 分页请求 */
@NullMarked
@Getter
@Setter
@Schema(description = "分页请求")
public class PageRequest {

  /** 当前页码 */
  @Schema(description = "当前页码", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
  private Integer pageIndex = 1;

  /** 每页大小 */
  @Schema(description = "每页大小", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
  private Integer pageSize = 10;
}
