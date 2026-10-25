package com.rongia.system.domain.employee;

import com.rongia.shared.domain.enums.BaseEnum;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/**
 * 婚姻状况枚举
 *
 * <p>Odoo {@code hr.employee.marital} 的选项由 {@code _get_marital_status_selection}（hr 模块的
 * 基础数据）提供、默认未婚；ERPNext {@code Employee.marital_status} 是可配的下拉。婚姻状况是封闭事实，
 * 我们取常见的四档（未婚 / 已婚 / 离异 / 丧偶），不建主数据。
 */
@NullMarked
@Getter
public enum MaritalStatusEnum implements BaseEnum {

  /** 未婚 */
  SINGLE("Single", "未婚"),

  /** 已婚 */
  MARRIED("Married", "已婚"),

  /** 离异 */
  DIVORCED("Divorced", "离异"),

  /** 丧偶 */
  WIDOWED("Widowed", "丧偶");

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
  MaritalStatusEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }
}
