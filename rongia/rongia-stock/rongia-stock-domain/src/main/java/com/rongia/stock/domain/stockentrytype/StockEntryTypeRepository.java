package com.rongia.stock.domain.stockentrytype;

import com.rongia.shared.domain.repository.Repository;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 库存单据类型仓储接口 */
@NullMarked
public interface StockEntryTypeRepository extends Repository<StockEntryType> {

  /**
   * 按编码取类型行
   *
   * <p>程序自动开单时用它定位：编码唯一，一行就是一个种类。
   *
   * @param code 类型编码
   * @return 类型行（可空），不存在时返回 null
   */
  @Nullable
  StockEntryType getByCode(String code);

  /**
   * 判断类型名称是否存在
   *
   * @param name 类型名称
   * @param id 排除的类型Id（可空，修改时传自己）
   * @return true 表示已存在
   */
  boolean isExistByNameExcludeId(String name, @Nullable Long id);

  /**
   * 判断类型名称是否存在（新增时校验）
   *
   * @param name 类型名称
   * @return true 表示已存在
   */
  default boolean isExistByNameExcludeId(String name) {
    return isExistByNameExcludeId(name, null);
  }
}
