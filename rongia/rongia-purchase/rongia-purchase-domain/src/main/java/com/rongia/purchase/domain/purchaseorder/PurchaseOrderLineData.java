package com.rongia.purchase.domain.purchaseorder;

import java.math.BigDecimal;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 采购单行数据
 *
 * @param materialId 物料SPU
 * @param variantId 物料SKU
 * @param supplierMaterialCode 供应商料号（可空）
 * @param qty 数量
 * @param unitId 单位Id
 * @param price 单价（不含税）
 * @param discount 折扣（%）
 * @param taxRateId 税率Id（可空）
 * @param taxRate 税率快照（百分比，可空）
 */
@NullMarked
public record PurchaseOrderLineData(
    Long materialId,
    Long variantId,
    @Nullable String supplierMaterialCode,
    BigDecimal qty,
    Long unitId,
    BigDecimal price,
    BigDecimal discount,
    @Nullable Long taxRateId,
    @Nullable BigDecimal taxRate) {}
