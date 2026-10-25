package com.rongia.inventory.domain.statuschange;

import com.rongia.shared.domain.repository.Repository;
import org.jspecify.annotations.NullMarked;

/** 库存状态转换单仓储接口 */
@NullMarked
public interface StockStatusChangeRepository extends Repository<StockStatusChange> {}
