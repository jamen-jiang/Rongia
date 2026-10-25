package com.rongia.shared.domain.entity;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 实体接口 */
@NullMarked
public interface Entity {
  /** 获取主键 */
  Long getId();
}
