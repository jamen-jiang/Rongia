package com.rongia.stock.domain.stockentry;

import java.math.BigDecimal;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 库存单据行数据
 *
 * <p>数量一律为正：普通单是流转多少，红字单是「要冲掉多少」。红字单里那个负号由单子自己落，外头填进来的永远
 * 是正数——省得谁把正负号填反了还看不出错在哪。出库仓库与入库仓库至少填一个：只填出库仓库是出库，只填入库仓库是入库，
 * 两个都填是调拨。
 *
 * @param materialId 物料SPU
 * @param variantId 物料SKU
 * @param qty 数量（正数）
 * @param outboundWarehouseId 出库仓库Id（可空）
 * @param inboundWarehouseId 入库仓库Id（可空）
 * @param sourceLineId 来源单据行Id（可空，指回工单用料行/销售单行/采购单行）
 * @param remark 备注（可空）
 */
@NullMarked
public record StockEntryLineData(
    Long materialId,
    Long variantId,
    BigDecimal qty,
    @Nullable Long outboundWarehouseId,
    @Nullable Long inboundWarehouseId,
    @Nullable Long sourceLineId,
    @Nullable String remark) {}
