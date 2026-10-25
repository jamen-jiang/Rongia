package com.rongia.basedata.domain.currency;

import com.rongia.shared.domain.repository.Repository;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 币种仓储接口 */
@NullMarked
public interface CurrencyRepository extends Repository<Currency> {

  /**
   * 按币种代码查询
   *
   * @param code 币种代码
   * @return 币种（可空），不存在返回 null
   */
  @Nullable Currency getByCode(String code);

  /**
   * 判断币种代码是否存在
   *
   * @param code 币种代码
   * @param id 排除的币种Id（可空，新增时传 null）
   * @return true 表示已存在
   */
  boolean isExistByCodeExcludeId(String code, @Nullable Long id);

  /**
   * 判断币种代码是否存在（新增时校验，无需排除Id）
   *
   * @param code 币种代码
   * @return true 表示已存在
   */
  default boolean isExistByCodeExcludeId(String code) {
    return isExistByCodeExcludeId(code, null);
  }
}
