package com.rongia.production.domain.operation;

import com.rongia.shared.domain.repository.Repository;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 工序仓储接口 */
@NullMarked
public interface OperationRepository extends Repository<Operation> {

  /**
   * 按代码查询
   *
   * @param code 工序代码
   * @return 工序（可空），不存在返回 null
   */
  @Nullable Operation getByCode(String code);

  /**
   * 判断工序代码是否存在
   *
   * @param code 工序代码
   * @param id 排除的工序Id（可空，新增时传 null）
   * @return true 表示已存在
   */
  boolean isExistByCodeExcludeId(String code, @Nullable Long id);

  /**
   * 判断工序代码是否存在
   *
   * @param code 工序代码
   * @return true 表示已存在
   */
  default boolean isExistByCodeExcludeId(String code) {
    return isExistByCodeExcludeId(code, null);
  }
}
