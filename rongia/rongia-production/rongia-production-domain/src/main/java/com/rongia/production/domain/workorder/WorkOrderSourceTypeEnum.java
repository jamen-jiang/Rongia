package com.rongia.production.domain.workorder;

import com.rongia.shared.domain.enums.BaseEnum;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/** 工单来源类型 */
@NullMarked
@Getter
public enum WorkOrderSourceTypeEnum implements BaseEnum {

  /** 销售订单：由销售订单行生成 */
  SALES_ORDER("SalesOrder", "销售订单"),

  /** 库存补货：备货生产，无销售订单 */
  STOCK("Stock", "库存补货"),

  /** 返工：由已完工工单返工生成 */
  REWORK("Rework", "返工");

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
  WorkOrderSourceTypeEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }
}
