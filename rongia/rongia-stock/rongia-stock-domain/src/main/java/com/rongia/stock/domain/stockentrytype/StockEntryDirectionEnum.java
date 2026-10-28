package com.rongia.stock.domain.stockentrytype;

import com.rongia.shared.domain.enums.BaseEnum;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/**
 * 库存单据方向
 *
 * <p>它说的是这张单的哪一头伸出仓库外面：入库是供应商那头，出库是客户那头。方向属于整张单，
 * 所以一张单要么都入、要么都出——每行都按同一个方向填。
 *
 * <p>调拨不在这里：它的两头都在系统里、哪一头都没伸出去，所以调拨不是一种方向，而是保留编码
 * {@code StockEntryTypeEnum#TRANSFER} 上的一种行形状。
 * 类型行必须挂一个方向，程序按方向校验，不看类型叫什么名字。
 */
@NullMarked
@Getter
public enum StockEntryDirectionEnum implements BaseEnum {

  /** 入库：只填入库库位，货进入仓库 */
  IN("In", "入库"),

  /** 出库：只填出库库位，货离开仓库 */
  OUT("Out", "出库");

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
