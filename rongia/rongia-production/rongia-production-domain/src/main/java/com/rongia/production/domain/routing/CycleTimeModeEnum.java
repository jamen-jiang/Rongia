package com.rongia.production.domain.routing;

import com.rongia.shared.domain.enums.BaseEnum;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/**
 * 周期时间计算方式
 *
 * <p>固定：工艺员手填一次做多长时间；按历史计算：取最近若干张已完工工单的实际周期时间取平均。
 */
@NullMarked
@Getter
public enum CycleTimeModeEnum implements BaseEnum {

  /** 固定：用手工填的工时 */
  MANUAL("Manual", "固定"),

  /** 按历史计算：取最近若干张工单的平均工时 */
  AUTO("Auto", "按历史计算");

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
  CycleTimeModeEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }
}
