package com.rongia.production.domain.productionreceipt;

import com.rongia.shared.domain.enums.BaseEnum;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/** 生产入库单状态 */
@NullMarked
@Getter
public enum ProductionReceiptStatusEnum implements BaseEnum {

  /** 草稿：车间刚把货报上来，库存和工单都还没动 */
  DRAFT("Draft", "草稿"),

  /** 已入库：实收数量真进了成品仓，单据落定 */
  POSTED("Posted", "已入库");

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
  ProductionReceiptStatusEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }
}
