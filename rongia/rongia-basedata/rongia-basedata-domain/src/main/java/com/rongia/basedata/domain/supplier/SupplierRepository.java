package com.rongia.basedata.domain.supplier;

import com.rongia.shared.domain.repository.Repository;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 供应商仓储接口 */
@NullMarked
public interface SupplierRepository extends Repository<Supplier> {

  /**
   * 按供应商代码查询
   *
   * @param code 供应商代码
   * @return 供应商（可空），不存在返回 null
   */
  @Nullable Supplier getByCode(String code);

  /**
   * 判断供应商代码是否存在
   *
   * @param code 供应商代码
   * @param id 排除的供应商Id（可空）
   * @return true 表示已存在
   */
  boolean isExistByCodeExcludeId(String code, @Nullable Long id);

  /**
   * 判断供应商代码是否存在（新增时校验）
   *
   * @param code 供应商代码
   * @return true 表示已存在
   */
  default boolean isExistByCodeExcludeId(String code) {
    return isExistByCodeExcludeId(code, null);
  }
}
