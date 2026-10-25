package com.rongia.production.domain.productionissue;

import java.math.BigDecimal;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 生产领料单行数据
 *
 * @param workOrderMaterialId 工单用料行Id
 * @param materialId 物料SPU
 * @param variantId 物料SKU
 * @param appliedQty 申请数量
 * @param unitId 单位Id
 * @param warehouseId 发料仓库Id
 * @param remark 备注（可空）
 */
@NullMarked
public record ProductionIssueLineData(
    Long workOrderMaterialId,
    Long materialId,
    Long variantId,
    BigDecimal appliedQty,
    Long unitId,
    Long warehouseId,
    @Nullable String remark) {}
