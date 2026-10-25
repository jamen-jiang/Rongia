package com.rongia.production.domain.bom;

import java.math.BigDecimal;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * BOM 行数据
 *
 * @param materialId 子件SPU
 * @param variantId 子件SKU
 * @param routingOperationId 消耗的路线工序Id（可空）
 * @param qty 单位用量（生产 1 个父件消耗的子件数量）
 * @param lossRate 损耗率（百分比，传 0 表示无损耗）
 * @param unitId 单位Id
 * @param sequence 行号
 */
@NullMarked
public record BomLineData(
    Long materialId,
    Long variantId,
    @Nullable Long routingOperationId,
    BigDecimal qty,
    BigDecimal lossRate,
    Long unitId,
    Integer sequence) {}
