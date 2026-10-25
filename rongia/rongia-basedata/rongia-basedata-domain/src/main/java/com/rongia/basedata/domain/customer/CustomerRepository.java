package com.rongia.basedata.domain.customer;

import com.rongia.shared.domain.repository.Repository;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 客户仓储接口 */
@NullMarked
public interface CustomerRepository extends Repository<Customer> {

  /**
   * 按客户代码查询
   *
   * @param code 客户代码
   * @return 客户（可空），不存在返回 null
   */
  @Nullable Customer getByCode(String code);

  /**
   * 判断客户代码是否存在
   *
   * @param code 客户代码
   * @param id 排除的客户Id（可空）
   * @return true 表示已存在
   */
  boolean isExistByCodeExcludeId(String code, @Nullable Long id);

  /**
   * 判断客户代码是否存在（新增时校验）
   *
   * @param code 客户代码
   * @return true 表示已存在
   */
  default boolean isExistByCodeExcludeId(String code) {
    return isExistByCodeExcludeId(code, null);
  }
}
