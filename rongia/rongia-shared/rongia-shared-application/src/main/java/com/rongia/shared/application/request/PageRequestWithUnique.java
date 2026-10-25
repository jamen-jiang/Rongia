package com.rongia.shared.application.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 带唯一值列表的分页请求
 *
 * @param <T> 唯一值类型
 */
@NullMarked
@Getter
@Setter
@Schema(description = "带唯一值列表的分页请求")
public class PageRequestWithUnique<T> extends PageRequest {

  /** 唯一值列表（可空） */
  @Schema(description = "唯一值列表")
  @Nullable
  private List<T> uniqueValues;
}
