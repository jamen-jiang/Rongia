package com.rongia.stock.domain.warehouse;

import com.rongia.shared.domain.repository.Repository;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 仓库仓储接口 */
@NullMarked
public interface WarehouseRepository extends Repository<Warehouse> {

  /**
   * 按代码查询
   *
   * @param code 仓库代码
   * @return 仓库（可空），不存在返回 null
   */
  @Nullable Warehouse getByCode(String code);

  /**
   * 判断仓库代码是否存在
   *
   * @param code 仓库代码
   * @param id 排除的仓库Id（可空，新增时传 null）
   * @return true 表示已存在
   */
  boolean isExistByCodeExcludeId(String code, @Nullable Long id);

  /**
   * 判断仓库代码是否存在
   *
   * @param code 仓库代码
   * @return true 表示已存在
   */
  default boolean isExistByCodeExcludeId(String code) {
    return isExistByCodeExcludeId(code, null);
  }
}
