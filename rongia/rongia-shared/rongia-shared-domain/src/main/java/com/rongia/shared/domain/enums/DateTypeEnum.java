package com.rongia.shared.domain.enums;

import java.time.LocalDateTime;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 日期类型枚举
 *
 * <p>流水号规则的计数重置周期用：规则上配了周期（本枚举编码），计数跨期归 1——按年重置每元旦
 * 从 1 开始，按月每月 1 号从 1 开始，按日每天从 1 开始。
 */
@NullMarked
@Getter
public enum DateTypeEnum implements BaseEnum {

  /** 年 */
  YEAR("Year", "年"),

  /** 月 */
  MONTH("Month", "月"),

  /** 日 */
  DAY("Day", "日");

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
  DateTypeEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }

  /**
   * 判断两次取号是否落在同一周期内
   *
   * @param last 上次取号时间（可空）
   * @param now 本次取号时间
   * @return 是否同周期（首次取号视为跨期，计数从初始值开始）
   */
  public boolean inSamePeriod(@Nullable LocalDateTime last, LocalDateTime now) {
    return switch (this) {
      case YEAR -> last != null && last.getYear() == now.getYear();
      case MONTH ->
          last != null
              && last.getYear() == now.getYear()
              && last.getMonthValue() == now.getMonthValue();
      case DAY -> last != null && last.toLocalDate().equals(now.toLocalDate());
    };
  }

  /**
   * 本周期要求前缀必含的日期变量
   *
   * <p>跨期后号重新从小号开始，前缀必须带上对应粒度的日期把不同周期的号区分开，否则单号会重复。
   *
   * @return 变量名：Year→{@code year}、Month→{@code month}、Day→{@code day}
   */
  public String requiredToken() {
    return switch (this) {
      case YEAR -> "year";
      case MONTH -> "month";
      case DAY -> "day";
    };
  }
}
