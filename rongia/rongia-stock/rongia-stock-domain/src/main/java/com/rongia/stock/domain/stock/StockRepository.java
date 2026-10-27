package com.rongia.stock.domain.stock;

import com.rongia.shared.domain.repository.Repository;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 库存仓储接口 */
@NullMarked
public interface StockRepository extends Repository<Stock> {

  /**
   * 按「SKU + 库位」取库存行
   *
   * @param variantId SKU
   * @param locationId 库位Id
   * @return 库存行（可空），该键还没有记录时返回 null
   */
  @Nullable
  Stock getByKey(long variantId, long locationId);
}
