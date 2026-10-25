package com.rongia.inventory.domain.stock;

import com.rongia.shared.domain.repository.Repository;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 库存仓储接口 */
@NullMarked
public interface StockRepository extends Repository<Stock> {

  /**
   * 按「SKU + 仓库」取库存行
   *
   * @param variantId SKU
   * @param warehouseId 仓库Id
   * @return 库存行（可空），该键还没有记录时返回 null
   */
  @Nullable
  Stock getByKey(long variantId, long warehouseId);
}
