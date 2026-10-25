package com.rongia.production.domain.routing;

import java.math.BigDecimal;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 路线工序数据
 *
 * <p>描述「这道工序在这条路线里怎么用」：顺序、工作中心、循环工时与计算方式。
 * 这些都是路线级决策——同一个镀膜工序，A 物料自制、B 物料外协，节拍也不同，所以不能挂在工序字典上。
 *
 * @param operationId 工序Id
 * @param workCenterId 工作中心Id
 * @param cycleTimeMode 周期时间计算方式编码
 * @param cycleTime 周期时间（分钟）
 * @param sequence 工序顺序
 */
@NullMarked
public record RoutingOperationData(
    Long operationId,
    Long workCenterId,
    String cycleTimeMode,
    BigDecimal cycleTime,
    Integer sequence) {}
