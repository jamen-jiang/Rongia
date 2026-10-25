package com.rongia.sales.domain.salesorder;

import com.rongia.shared.domain.enums.BaseEnum;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/** 销售订单状态 */
@NullMarked
@Getter
public enum SalesOrderStatusEnum implements BaseEnum {

  /** 草稿：还能改客户、改行、改价 */
  DRAFT("Draft", "草稿"),

  /** 待发货：单子已算数，客户与价格定死，等着出货 */
  TO_DELIVER("ToDeliver", "待发货"),

  /** 已完成：货全发完了，这单的活干完了 */
  COMPLETED("Completed", "已完成"),

  /** 已关闭：结案，不再允许变更（剩余未发的额度一并作罢） */
  CLOSED("Closed", "已关闭"),

  /** 已取消：客户不做了 */
  CANCELLED("Cancelled", "已取消");

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
  SalesOrderStatusEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }
}
