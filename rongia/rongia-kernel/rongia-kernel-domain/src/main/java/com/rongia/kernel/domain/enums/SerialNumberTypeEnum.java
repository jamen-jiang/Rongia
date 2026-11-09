package com.rongia.kernel.domain.enums;

import com.rongia.shared.enums.BaseEnum;

import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/**
 * 流水号类型枚举
 *
 * <p>编码沿用实体表名（{@code WorkOrder} / {@code StockEntry}），与
 * {@code OrderAddedEvent} 里带出去的类型串一致，流水号规则按这个编码挂单号前缀与编号段。一张表一个编码，表里的
 * 红字单、普通单共用一个号段。
 */
@NullMarked
@Getter
public enum SerialNumberTypeEnum implements BaseEnum {

  /** 销售订单 */
  SALES_ORDER("SalesOrder", "销售订单"),

  /** 采购单 */
  PURCHASE_ORDER("PurchaseOrder", "采购单"),

  /** 工单（生产订单） */
  WORK_ORDER("WorkOrder", "工单"),

  /** 工卡（作业卡，一张卡对应工单上的一道工序） */
  JOB_CARD("JobCard", "工卡"),

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
