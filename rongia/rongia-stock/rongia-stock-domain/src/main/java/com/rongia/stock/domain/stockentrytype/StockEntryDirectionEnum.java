package com.rongia.stock.domain.stockentrytype;

import com.rongia.shared.domain.enums.BaseEnum;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/**
 * 库存单据方向
 *
 * <p>它决定一张单的库位该怎么填：入库只填入库库位、出库只填出库库位、出入两头都填。
 * 类型行必须挂一个方向，程序按方向校验，不看类型叫什么名字。
 */
@NullMarked
@Getter
public enum StockEntryDirectionEnum implements BaseEnum {

  /** 入库：只填入库库位，货进入仓库 */
  IN("In", "入库"),

  /** 出库：只填出库库位，货离开仓库 */
  OUT("Out", "出库"),

  /** 出入：出库库位与入库库位都填，一张单两头记账（调拨、分装、拆解这类） */
  BOTH("Both", "出入");

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
  StockEntryDirectionEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }
}
