package com.rongia.inventory.domain.stockrecord;

import com.rongia.shared.domain.enums.BaseEnum;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/**
 * 库存记录的出入方向
 *
 * <p>整张单一个方向，所以挂在单据头上：入库单就是货进仓，出库单就是货出仓。一张单里又要进又要出的（调拨的两行、
 * 盘点的盈亏行）现在没有；真有那一天，按别家做法是在行上带调出仓与调入仓，而不是给行加一个方向列。
 *
 * <p>红字单不改方向，只把数量取负——红字入库就是「入库」方向上的负数。这样月底算入库合计时它会自然抵掉，
 * 而不是跑到出库合计里去；只看正负号分方向的算法在这里是错的。
 */
@NullMarked
@Getter
public enum StockRecordDirectionEnum implements BaseEnum {

  /** 入库：货进到仓里（采购收货、完工入库、退料回库、盘盈） */
  IN("In", "入库"),

  /** 出库：货离开仓库（领料、销售发货、盘亏） */
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
  StockRecordDirectionEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }
}
