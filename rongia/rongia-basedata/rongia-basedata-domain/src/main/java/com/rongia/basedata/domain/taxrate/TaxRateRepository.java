package com.rongia.basedata.domain.taxrate;

import com.rongia.shared.domain.repository.Repository;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 税率仓储接口 */
@NullMarked
public interface TaxRateRepository extends Repository<TaxRate> {

  /**
   * 按代码查询
   *
   * @param code 税率代码
   * @return 税率（可空），不存在返回 null
   */
  @Nullable TaxRate getByCode(String code);

  /**
   * 判断税率代码是否存在
   *
   * @param code 税率代码
   * @param id 排除的税率Id（可空，新增时传 null）
   * @return true 表示已存在
   */
  boolean isExistByCodeExcludeId(String code, @Nullable Long id);

  /**
   * 判断税率代码是否存在
   *
   * @param code 税率代码
   * @return true 表示已存在
   */
  default boolean isExistByCodeExcludeId(String code) {
    return isExistByCodeExcludeId(code, null);
  }
}
