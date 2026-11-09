package com.rongia.shared.domain.enums;

import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/** 输入类型 */
@NullMarked
@Getter
public enum InputTypeEnum implements BaseEnum {

  /** 文本输入框 */
  TEXT("Text", "文本输入框"),

  /** 数字输入框 */
  NUMBER("Number", "数字输入框"),

  /** 日期选择框 */
  DATETIME("DateTime", "日期选择框"),

  /** 下拉单选 */
  SELECT("Select", "下拉单选"),

  /** 下拉多选 */
  MULTI_SELECT("MultiSelect", "下拉多选"),

  /** 复选框 */
  CHECKBOX("Checkbox", "复选框"),

  /** 单选框 */
  RADIO("Radio", "单选框"),

  /** 开关 */
  SWITCH("Switch", "开关");

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
  InputTypeEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }
}
