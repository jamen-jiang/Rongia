package com.rongia.production.domain.workcenter;

import com.rongia.shared.domain.enums.BaseEnum;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/**
 * 工作中心类型
 *
 * <p>照 Odoo 19 {@code resource.resource.resource_type} 的两档，但换成生产侧的说法：
 * {@code equipment} 设备（镀膜线、抛光机），产能按台数算；{@code team} 班组（抛光组、包装组），
 * 产能按人数算。两者日历工时的口径一样，但产能上限与成本归类不同——设备的时薪是折旧加电费，
 * 班组的是人工费。
 */
@NullMarked
@Getter
public enum WorkCenterTypeEnum implements BaseEnum {

  /** 设备 */
  EQUIPMENT("Equipment", "设备"),

  /** 班组 */
  TEAM("Team", "班组");

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
  WorkCenterTypeEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }
}
