package com.rongia.shared.application.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/**
 * 分页响应
 *
 * @param <T> 数据类型
 */
@NullMarked
@Getter
@Schema(description = "分页响应")
public class PageResponse<T> {

  /** 数据总数 */
  @Schema(description = "数据总数")
  private final Integer totalCount;

  /** 每页大小 */
  @Schema(description = "每页大小")
  private final Integer pageSize;

  /** 当前页码 */
  @Schema(description = "当前页码")
  private final Integer pageIndex;

  /** 列表 */
  @Schema(description = "列表")
  private final List<T> list;

  /**
   * 构造器
   *
   * @param totalCount 数据总数
   * @param pageSize 每页大小
   * @param pageIndex 当前页码
   * @param list 列表
   */
  public PageResponse(Integer totalCount, Integer pageSize, Integer pageIndex, List<T> list) {
    this.totalCount = totalCount;
    this.pageSize = pageSize;
    this.pageIndex = pageIndex;
    this.list = list;
  }
}
