package com.rongia.shared.domain.repository;

import com.rongia.shared.domain.aggregate.AggregateRoot;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 仓储基础接口 */
@NullMarked
public interface Repository<T extends AggregateRoot> {

  /**
   * 按主键查询
   *
   * @param id 主键
   * @return 聚合根，不存在返回 null
   */
  @Nullable T get(long id);

  /**
   * 保存（新增或更新）
   *
   * @param aggregateRoot 聚合根
   */
  void save(T aggregateRoot);
}
