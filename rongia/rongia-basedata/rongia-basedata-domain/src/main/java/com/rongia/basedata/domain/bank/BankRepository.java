package com.rongia.basedata.domain.bank;

import com.rongia.shared.domain.repository.Repository;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 银行仓储接口 */
@NullMarked
public interface BankRepository extends Repository<Bank> {

  /**
   * 按银行代码查询
   *
   * @param code 银行代码
   * @return 银行（可空），不存在返回 null
   */
  @Nullable Bank getByCode(String code);

  /**
   * 判断银行代码是否存在
   *
   * @param code 银行代码
   * @param id 排除的银行Id（可空，新增时传 null）
   * @return true 表示已存在
   */
  boolean isExistByCodeExcludeId(String code, @Nullable Long id);

  /**
   * 判断银行代码是否存在（新增时校验）
   *
   * @param code 银行代码
   * @return true 表示已存在
   */
  default boolean isExistByCodeExcludeId(String code) {
    return isExistByCodeExcludeId(code, null);
  }
}
