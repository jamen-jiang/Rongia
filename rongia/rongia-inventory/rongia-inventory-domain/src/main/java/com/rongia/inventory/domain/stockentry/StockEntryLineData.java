package com.rongia.inventory.domain.stockentry;

import java.math.BigDecimal;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 库存单据行数据
 *
 * <p>数量一律为正：普通单是流转多少，红字单是「要冲掉多少」。红字单里那个负号由单子自己落，外头填进来的永远
 * 是正数——省得谁把正负号填反了还看不出错在哪。源仓与目标仓至少填一个：只填源仓是出仓，只填目标仓是进仓，
 * 两个都填是调拨。
 *
 * @param materialId 物料SPU
 * @param variantId 物料SKU
 * @param qty 数量（正数）
 * @param sourceWarehouseId 源仓库Id（可空）
 * @param targetWarehouseId 目标仓库Id（可空）
 * @param remark 备注（可空）
 */
@NullMarked
public record StockEntryLineData(
    Long materialId,
    Long variantId,
    BigDecimal qty,
    @Nullable Long sourceWarehouseId,
    @Nullable Long targetWarehouseId,
    @Nullable String remark) {}
