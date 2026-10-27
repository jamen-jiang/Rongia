package com.rongia.stock.domain.stockentrytype;

import com.rongia.shared.domain.enums.BaseEnum;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/**
 * 库存单据类型编码（程序内部常量）
 *
 * <p>程序自动开单时按库位与来源推出编码——两个库位都填是调拨，只填出库库位是领料出库，只填入库库位时看来源：
 * 来源是工单就是完工入库，否则是采购入库。这四个编码自带方向，程序不用查类型表就知道该往哪走。
 *
 * <p>它和 {@code StockEntryType} 表没有结构绑定：表里的编码是用户自建的，方向由表上的 {@code Direction}
 * 决定；只有编码恰好落在这四个里时，才以枚举的方向为准。
 */
@NullMarked
@Getter
public enum StockEntryTypeEnum implements BaseEnum {

  /** 领料出库：只填出库库位，货离开仓库 */
  PRODUCTION_ISSUE("ProductionIssue", "领料出库", StockEntryDirectionEnum.OUT),

  /** 采购入库：只填入库库位，来源不是工单（采购到货、其他入库） */
  PURCHASE_RECEIPT("PurchaseReceipt", "采购入库", StockEntryDirectionEnum.IN),

  /** 完工入库：只填入库库位，来源是工单 */
  PRODUCTION_RECEIPT("ProductionReceipt", "完工入库", StockEntryDirectionEnum.IN),

  /** 调拨：出库库位与入库库位都填，一张单两头记账 */
  TRANSFER("Transfer", "调拨", StockEntryDirectionEnum.BOTH);

  /** 编码 */
  private final String code;

  /** 标签 */
  private final String label;

  /** 方向：编码本身就决定了库位该填哪一端 */
  private final StockEntryDirectionEnum direction;

  /**
   * 构造器
   *
   * @param code 编码
   * @param label 标签
   * @param direction 方向
   */
  StockEntryTypeEnum(String code, String label, StockEntryDirectionEnum direction) {
    this.code = code;
    this.label = label;
    this.direction = direction;
  }
}
