package com.rongia.basedata.domain.customergroup;

import com.rongia.shared.domain.repository.Repository;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 客户组仓储接口 */
@NullMarked
public interface CustomerGroupRepository extends Repository<CustomerGroup> {

  /**
   * 按代码查询
   *
   * @param code 客户组代码
   * @return 客户组（可空），不存在返回 null
   */
  @Nullable
  CustomerGroup getByCode(String code);

  /**
   * 判断代码是否存在
   *
   * @param code 客户组代码
   * @param id 排除的客户组Id（可空，新增时传 null）
   * @return true 表示已存在
   */
  boolean isExistByCodeExcludeId(String code, @Nullable Long id);

  /**
   * 判断代码是否存在
   *
   * @param code 客户组代码
   * @return true 表示已存在
   */
  default boolean isExistByCodeExcludeId(String code) {
    return isExistByCodeExcludeId(code, null);
  }
}
