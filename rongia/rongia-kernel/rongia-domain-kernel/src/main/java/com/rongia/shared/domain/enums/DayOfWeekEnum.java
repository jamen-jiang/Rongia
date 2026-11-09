package com.rongia.shared.domain.enums;

import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/**
 * 星期
 *
 * <p>编码与 {@link java.time.DayOfWeek#name()} 一致，算可用工时时直接按名字对上，
 * 不需要维护一张「1 = 周一」的映射表。
 */
@NullMarked
@Getter
public enum DayOfWeekEnum implements BaseEnum {

  /** 周一 */
  MONDAY("Monday", "周一"),

  /** 周二 */
  TUESDAY("Tuesday", "周二"),

  /** 周三 */
  WEDNESDAY("Wednesday", "周三"),

  /** 周四 */
  THURSDAY("Thursday", "周四"),

  /** 周五 */
  FRIDAY("Friday", "周五"),

  /** 周六 */
  SATURDAY("Saturday", "周六"),

  /** 周日 */
  SUNDAY("Sunday", "周日");

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
  DayOfWeekEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }

  /**
   * 取 Java 的星期枚举
   *
   * @return 对应的 {@link java.time.DayOfWeek}
   */
  public java.time.DayOfWeek toDayOfWeek() {
    return java.time.DayOfWeek.valueOf(this.code.toUpperCase());
  }
}
