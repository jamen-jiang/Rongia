package com.rongia.shared.application.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.jspecify.annotations.NullMarked;

/**
 * 分页响应
 *
 * @param totalCount 数据总数
 * @param pageSize 每页大小
 * @param pageIndex 当前页码
 * @param list 列表
 * @param <T> 数据类型
 */
@NullMarked
@Schema(description = "分页响应")
public record PageResponse<T>(
    @Schema(description = "数据总数") Integer totalCount,
    @Schema(description = "每页大小") Integer pageSize,
    @Schema(description = "当前页码") Integer pageIndex,
    @Schema(description = "列表") List<T> list) {}
