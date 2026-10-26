package com.rongia.production.domain.routing;

import java.math.BigDecimal;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 路线工序数据
 *
 * <p>描述「这道工序在这条路线上怎么用」：顺序、工作中心、工时与批量。
 *
 * <p>工作中心、工时、批量、费率可空：空表示「取默认值」（前三个取工序字典、费率取工作中心），
 * 非空表示这一行要覆盖。
 *
 * @param operationId 工序Id
 * @param workCenterId 工作中心Id（可空，空则取工序默认值）
 * @param cycleTime 周期时间（分钟，可空，空则取工序默认值）
 * @param batchSize 批量大小（件，可空，空则取工序默认值）
 * @param hourlyRate 小时费率（元/小时，可空，空则取工作中心费率）
 * @param sequence 工序顺序
 */
@NullMarked
public record RoutingOperationData(
    Long operationId,
    @Nullable Long workCenterId,
    @Nullable BigDecimal cycleTime,
    @Nullable BigDecimal batchSize,
    @Nullable BigDecimal hourlyRate,
    Integer sequence) {}
