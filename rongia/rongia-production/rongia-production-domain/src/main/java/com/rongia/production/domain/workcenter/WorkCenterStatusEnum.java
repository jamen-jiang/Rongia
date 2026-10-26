package com.rongia.production.domain.workcenter;

import com.rongia.shared.domain.enums.BaseEnum;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/**
 * 工作中心状态
 *
 * <p>回答「这台中心此刻在干什么」，六档照抄 ERPNext 的 Workstation.status：Production/Idle/Setup/Maintenance/Problem/Off。
 * 它由车间现场人工改，排产读它判断今天敢不敢接新活，不参与对账。
 *
 * <p>Odoo 那边只有三档（Normal/Blocked/In Progress），而且是从工时记录算出来的；这里按人填的粒度取 ERPNext 的六档，
 * 「为什么停」直接体现在状态上，不必再挂停机原因主数据。
 */
@NullMarked
@Getter
public enum WorkCenterStatusEnum implements BaseEnum {

  /** 生产中：有工单正在这台中心上干 */
  PRODUCTION("Production", "生产中"),

  /** 闲置：没在干活，随时可以接新活 */
  IDLE("Idle", "闲置"),

  /** 换型调试中：正在为下一批做换型准备，暂不接新活 */
  SETUP("Setup", "换型调试"),

  /** 维护保养：计划内的保养，暂不接新活 */
  MAINTENANCE("Maintenance", "维护保养"),

  /** 故障：设备出了问题，新工单点不开 */
  PROBLEM("Problem", "故障"),

  /** 停用：关机或不再使用，不参与排产 */
  OFF("Off", "停用");

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
  WorkCenterStatusEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }
}
