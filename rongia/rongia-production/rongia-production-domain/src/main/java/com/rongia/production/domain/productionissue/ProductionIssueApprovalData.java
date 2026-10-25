package com.rongia.production.domain.productionissue;

import java.math.BigDecimal;
import org.jspecify.annotations.NullMarked;

/**
 * 生产领料单审核时逐行给定的核定数量
 *
 * @param workOrderMaterialId 工单用料行Id
 * @param approvedQty 核定数量
 */
@NullMarked
public record ProductionIssueApprovalData(Long workOrderMaterialId, BigDecimal approvedQty) {}
