package com.rongia.sales.domain.salesorder;

import java.math.BigDecimal;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 销售订单行数据
 *
 * @param sequence 行号
 * @param materialId 物料SPU
 * @param variantId 物料SKU
 * @param customerMaterialCode 客户料号（可空）
 * @param customerMaterialName 客户叫法（可空）
 * @param qty 数量
 * @param unitId 单位Id
 * @param price 单价（不含税）
 * @param discount 折扣（%）
 * @param taxRateId 税率Id（可空）
 * @param taxRate 税率快照（百分比，可空）
 */
@NullMarked
public record SalesOrderLineData(
    int sequence,
    Long materialId,
    Long variantId,
    @Nullable String customerMaterialCode,
    @Nullable String customerMaterialName,
    BigDecimal qty,
    Long unitId,
    BigDecimal price,
    BigDecimal discount,
    @Nullable Long taxRateId,
    @Nullable BigDecimal taxRate) {}
