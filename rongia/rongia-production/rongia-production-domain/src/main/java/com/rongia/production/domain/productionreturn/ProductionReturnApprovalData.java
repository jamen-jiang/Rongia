package com.rongia.production.domain.productionreturn;

import java.math.BigDecimal;
import org.jspecify.annotations.NullMarked;

/**
 * 生产退料单审核时逐行给定的核定数量
 *
 * @param workOrderMaterialId 工单用料行Id
 * @param approvedQty 核定退料数量
 */
@NullMarked
public record ProductionReturnApprovalData(Long workOrderMaterialId, BigDecimal approvedQty) {}
