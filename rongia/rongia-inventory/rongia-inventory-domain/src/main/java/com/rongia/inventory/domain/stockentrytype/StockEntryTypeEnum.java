package com.rongia.inventory.domain.stockentrytype;

import com.rongia.shared.domain.enums.BaseEnum;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/**
 * 库存单据类型编码
 *
 * <p>程序生成单据时按行上的仓推出编码——源仓与目标仓都填是调拨，只填源仓是领料出库，只填目标仓是收货入库
 * （ERPNext 的 {@code make_stock_entry} 原样照搬：仓怎么填推 purpose）。编码再反查类型表里
 * {@code isStandard} 为 true 的那一行落到单头上，给单子归堆。
 *
 * <p>编码是程序认的唯一名，永不改；类型表里同名行的名称、排序随系统维护改。要加新种类（如「报废出库」），
 * 往类型表加非标准行就行，标准编码表不动——标准编码只覆盖程序会自动生成的那些单。
 */
@NullMarked
@Getter
public enum StockEntryTypeEnum implements BaseEnum {

  /** 领料出库：只填源仓，货离开仓库 */
  MATERIAL_ISSUE("MaterialIssue", "领料出库"),

  /** 收货入库：只填目标仓，货进入仓库 */
  MATERIAL_RECEIPT("MaterialReceipt", "收货入库"),

  /** 调拨：源仓与目标仓都填，一张单两头记账 */
  MATERIAL_TRANSFER("MaterialTransfer", "调拨");

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
  StockEntryTypeEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }
}
