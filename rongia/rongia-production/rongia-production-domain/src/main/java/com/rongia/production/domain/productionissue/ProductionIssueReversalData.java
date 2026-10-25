package com.rongia.production.domain.productionissue;

import java.math.BigDecimal;
import org.jspecify.annotations.NullMarked;

/**
 * 生产领料单红冲时逐行给定的冲回数量
 *
 * <p>数量一律为正，与红字库存单上填的数一致；负号只在库存那边落。
 *
 * @param workOrderMaterialId 工单用料行Id
 * @param qty 本次冲回数量（正数）
 */
@NullMarked
public record ProductionIssueReversalData(Long workOrderMaterialId, BigDecimal qty) {}
