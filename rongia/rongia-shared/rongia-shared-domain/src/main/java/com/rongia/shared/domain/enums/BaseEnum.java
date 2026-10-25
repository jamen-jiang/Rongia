package com.rongia.shared.domain.enums;

import org.jspecify.annotations.NullMarked;

/** 枚举基础接口 */
@NullMarked
public interface BaseEnum {

  /**
   * 获取编码
   *
   * @return 编码
   */
  String getCode();

  /**
   * 获取标签
   *
   * @return 标签
   */
  String getLabel();
}
