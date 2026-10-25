package com.rongia.shared.domain.enums;

import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/** 数据类型 */
@NullMarked
@Getter
public enum DataTypeEnum implements BaseEnum {

  /** 字符串 */
  STRING("String", "字符串"),

  /** 整数 */
  INTEGER("Integer", "整数"),

  /** 小数 */
  DECIMAL("Decimal", "小数"),

  /** 布尔值 */
  BOOLEAN("Boolean", "布尔值"),

  /** 日期 */
  DATETIME("DateTime", "日期");

  /** 编码 */
  private final String code;

  /** 标签 */
  private final String label;

  /**
   * 构造器
   *
   * @param code 编码
   * @param label 标签
   */
  DataTypeEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }
}
