package com.rongia.shared.domain.entity;

import org.jspecify.annotations.NullMarked;

/** 支持逻辑删除（软删除）的实体基类 适用于需要"假删除"功能的业务实体 */
@NullMarked
public interface DeleteEntity {

  /** 获取是否已删除 */
  Boolean getIsDeleted();

  /** 标记为已删除（逻辑删除） 子类可重写以触发领域事件 */
  void delete();
}
