package com.rongia.shared.domain.entity;

import java.time.LocalDateTime;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 支持更新审计信息的实体接口 记录更新人、更新人名称、更新时间 */
@NullMarked
public interface UpdateEntity {
  /** 获取更新人Id */
  @Nullable Long getUpdatedBy();

  /** 获取更新人名称 */
  @Nullable String getUpdatedByName();

  /** 获取更新时间 */
  @Nullable LocalDateTime getUpdatedTime();

  /**
   * 设置更新审计信息 子类可重写以触发领域事件
   *
   * @param userId 更新人Id
   * @param userName 更新人名称
   */
  void update(long userId, String userName);
}
