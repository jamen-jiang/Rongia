package com.rongia.system.domain.employee;

import com.rongia.shared.domain.enums.BaseEnum;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/**
 * 性别枚举
 *
 * <p>取值照 Odoo {@code hr.employee.gender}（male / female / other）；ERPNext 的 {@code gender} 是必填的
 * 独立主数据，但性别没有可配置的必要，做成枚举就够。
 */
@NullMarked
@Getter
public enum GenderEnum implements BaseEnum {

  /** 男 */
  MALE("Male", "男"),

  /** 女 */
  FEMALE("Female", "女"),

  /** 其他 */
  OTHER("Other", "其他");

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
  GenderEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }
}
