package com.rongia.production.domain.routing;

import java.math.BigDecimal;
import org.jspecify.annotations.NullMarked;

/**
 * 路线工序数据
 *
 * <p>描述「这道工序在这条路线上怎么用」：顺序、工作中心、三段工时。工作中心与三段工时都必须给——
 * 工序字典上不带这些数，费率归工作中心，批量归工作中心的批次上限。
 *
 * @param operationId 工序Id
 * @param workCenterId 工作中心Id
 * @param setupTime 准备时间（分钟，每批一次）
 * @param laborTime 人工时间（分钟/件）
 * @param machineTime 机器时间（分钟/件）
 * @param sequence 工序顺序
 */
@NullMarked
public record RoutingOperationData(
    Long operationId,
    Long workCenterId,
    BigDecimal setupTime,
    BigDecimal laborTime,
    BigDecimal machineTime,
    Integer sequence) {}
