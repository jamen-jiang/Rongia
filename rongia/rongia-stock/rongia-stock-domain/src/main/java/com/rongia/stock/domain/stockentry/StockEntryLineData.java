package com.rongia.stock.domain.stockentry;

import java.math.BigDecimal;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 库存单据行数据
 *
 * <p>数量一律为正：普通单是流转多少，红字单是「要冲掉多少」。红字单里那个负号由单子自己落，外头填进来的永远
 * 是正数——省得谁把正负号填反了还看不出错在哪。出库库位与入库库位至少填一个：只填出库库位是出仓，只填入库库位是进仓，
 * 两个都填是调拨。
 *
 * @param materialId 物料SPU
 * @param variantId 物料SKU
 * @param qty 数量（正数）
 * @param outboundLocationId 出库库位Id（可空）
 * @param inboundLocationId 入库库位Id（可空）
 * @param remark 备注（可空）
 */
@NullMarked
public record StockEntryLineData(
    Long materialId,
    Long variantId,
    BigDecimal qty,
    @Nullable Long outboundLocationId,
    @Nullable Long inboundLocationId,
    @Nullable String remark) {}
