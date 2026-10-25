package com.rongia.inventory.domain.stock;

import com.rongia.shared.domain.repository.Repository;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 库存仓储接口 */
@NullMarked
public interface StockRepository extends Repository<Stock> {

  /**
   * 按「SKU + 仓库 + 库存状态」取库存行
   *
   * <p>状态不可空，同一个位置上的待检与可用是两行。
   *
   * @param variantId SKU
   * @param warehouseId 仓库Id
   * @param stockStatus 库存状态编码
   * @return 库存行（可空），该键还没有记录时返回 null
   */
  @Nullable
  Stock getByKey(long variantId, long warehouseId, String stockStatus);
}
