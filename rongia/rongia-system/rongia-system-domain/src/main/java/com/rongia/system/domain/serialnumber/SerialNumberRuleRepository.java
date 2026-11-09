package com.rongia.system.domain.serialnumber;

import com.rongia.kernel.domain.repository.Repository;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 流水号规则仓储接口 */
@NullMarked
public interface SerialNumberRuleRepository extends Repository<SerialNumberRule> {

  /**
   * 按类型查询规则
   *
   * @param type 类型
   * @return 规则，不存在返回 null
   */
  @Nullable SerialNumberRule getByType(@Nullable String type);

  /**
   * 按类型悲观锁查询规则（消费计数用，必须在事务中调用）
   *
   * @param type 类型
   * @return 规则，不存在返回 null
   */
  @Nullable SerialNumberRule getByTypeForUpdate(@Nullable String type);

  /**
   * 判断类型是否已存在（可排除自身）
   *
   * @param type 类型
   * @param id 排除的Id（用于更新时校验，可为空）
   * @return 是否存在
   */
  boolean isExistByTypeExcludeId(@Nullable String type, @Nullable Long id);

  /**
   * 判断类型是否已存在（新增时校验）
   *
   * @param type 类型
   * @return 是否存在
   */
  default boolean isExistByTypeExcludeId(@Nullable String type) {
    return isExistByTypeExcludeId(type, null);
  }
}
