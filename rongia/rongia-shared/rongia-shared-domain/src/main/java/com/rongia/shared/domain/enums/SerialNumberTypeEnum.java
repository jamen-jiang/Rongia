package com.rongia.shared.domain.enums;

import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/**
 * 流水号类型枚举
 *
 * <p>编码沿用实体表名（{@code WorkOrder} / {@code WorkOrderTracking}），与
 * {@code OrderAddedEvent} 里带出去的类型串一致，流水号规则按这个编码挂单号前缀与编号段。一张表一个编码，表里的
 * 红字单、普通单共用一个号段。
 */
@NullMarked
@Getter
public enum SerialNumberTypeEnum implements BaseEnum {

  /** 销售订单 */
  SALES_ORDER("SalesOrder", "销售订单"),

  /** 工单（生产订单） */
  WORK_ORDER("WorkOrder", "工单"),

  /** 生产报工单 */
  WORK_ORDER_TRACKING("WorkOrderTracking", "工单跟踪单（报工）"),

  /** 库存单据（通用出入库单，红字单也用这一段号） */
  STOCK_ENTRY("StockEntry", "库存单据");

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
  SerialNumberTypeEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }
}
