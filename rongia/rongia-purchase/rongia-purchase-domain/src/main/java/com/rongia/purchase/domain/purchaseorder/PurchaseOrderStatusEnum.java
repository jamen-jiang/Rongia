package com.rongia.purchase.domain.purchaseorder;

import com.rongia.shared.domain.enums.BaseEnum;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/** 采购单状态 */
@NullMarked
@Getter
public enum PurchaseOrderStatusEnum implements BaseEnum {

  /** 草稿：还能改供应商、改行、改价 */
  DRAFT("Draft", "草稿"),

  /** 已确认：单子算数了，供应商与价格定死，等着收货 */
  CONFIRMED("Confirmed", "已确认"),

  /** 已关闭：结案，不再允许变更（收过的照算，剩余未到的额度作罢） */
  CLOSED("Closed", "已关闭"),

  /** 已取消：不买了 */
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
  PurchaseOrderStatusEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }
}
