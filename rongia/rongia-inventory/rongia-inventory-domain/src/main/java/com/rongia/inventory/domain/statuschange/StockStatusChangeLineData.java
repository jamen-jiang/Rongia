package com.rongia.inventory.domain.statuschange;

import java.math.BigDecimal;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 库存状态转换单行数据
 *
 * @param materialId 物料Id（快照）
 * @param variantId 规格型号Id
 * @param warehouseId 仓库Id
 * @param fromStockStatus 转出状态编码
 * @param toStockStatus 转入状态编码
 * @param qty 挪动数量（正数）
 * @param remark 备注（可空）
 */
@NullMarked
public record StockStatusChangeLineData(
    Long materialId,
    Long variantId,
    Long warehouseId,
    String fromStockStatus,
    String toStockStatus,
    BigDecimal qty,
    @Nullable String remark) {}
