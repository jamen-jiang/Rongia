package com.rongia.system.infrastructure.serialnumber;

import com.rongia.system.domain.serialnumber.SerialNumberRule;
import com.rongia.system.domain.serialnumber.SerialNumberRuleRepository;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

/** 流水号规则仓储实现 */
@NullMarked
@Repository
public class SerialNumberRuleRepositoryImpl implements SerialNumberRuleRepository {

  /** JPA 接口 */
  private final SerialNumberRuleJpa jpa;

  /**
   * 构造器注入
   *
   * @param jpa JPA 接口
   */
  public SerialNumberRuleRepositoryImpl(SerialNumberRuleJpa jpa) {
    this.jpa = jpa;
  }

  /**
   * 按主键查询
   *
   * @param id 主键
   * @return 规则，不存在返回 null
   */
  @Override
  @Nullable
  public SerialNumberRule get(long id) {
    return jpa.findById(id).orElse(null);
  }

  /**
   * 按类型查询规则
   *
   * @param type 类型
   * @return 规则，不存在返回 null
   */
  @Override
  @Nullable
  public SerialNumberRule getByType(@Nullable String type) {
    return jpa.findByType(type);
  }

  /**
   * 按类型悲观锁查询规则（必须在事务中调用）
   *
   * @param type 类型
   * @return 规则，不存在返回 null
   */
  @Override
  @Nullable
  public SerialNumberRule getByTypeForUpdate(@Nullable String type) {
    return jpa.findByTypeForUpdate(type);
  }

  /**
   * 判断类型是否已存在（可排除指定Id）
   *
   * @param type 类型
   * @param id 排除的规则Id（可空）
   * @return true 表示已存在
   */
  @Override
  public boolean isExistByTypeExcludeId(@Nullable String type, @Nullable Long id) {
    return jpa.existsByTypeExcludeId(type, id);
  }

  /**
   * 保存（新增或更新）
   *
   * @param model 规则聚合
   */
  @Override
  public void save(SerialNumberRule model) {
    jpa.save(model);
  }
}
