package com.rongia.shared.domain.entity;

import java.time.LocalDateTime;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 支持创建审计信息的实体接口 记录创建人、创建人名称、创建时间 */
@NullMarked
public interface CreateEntity {
  /** 获取创建人Id */
  Long getCreatedBy();

  /** 获取创建人名称 */
  String getCreatedByName();

  /** 获取创建时间 */
  LocalDateTime getCreatedTime();

  /**
   * 设置创建审计信息 子类可重写以触发领域事件
   *
   * @param userId 创建人Id
   * @param userName 创建人名称
   * @param dateTime 创建时间（可空，为空时取当前时间）
   */
  void create(long userId, String userName, @Nullable LocalDateTime dateTime);

  /**
   * 设置创建审计信息（使用当前时间）
   *
   * @param userId 创建人Id
   * @param userName 创建人名称
   */
  void create(long userId, String userName);
}
