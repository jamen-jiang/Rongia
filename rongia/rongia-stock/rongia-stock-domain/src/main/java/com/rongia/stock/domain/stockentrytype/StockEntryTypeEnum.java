package com.rongia.stock.domain.stockentrytype;

import com.rongia.shared.domain.enums.BaseEnum;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/**
 * 库存单据类型编码（程序内部常量）
 *
 * <p>程序自动开单时按仓库与来源推出编码——两个仓库都填是调拨；只填一端时看来源：出库是销售单就是销售出库、
 * 是采购单就是采购退货、否则是领料出库；入库是销售单就是销售退货、是工单就是完工入库、否则是采购入库。
 * 这八个编码是程序保留的，用户建类型时占不到。
 *
 * <p>生产退料不在自动推码里：它跟完工入库同为「入库 + 来源是工单」，自动分不出来，开单时必须显式指定类型编码。
 *
 * <p>方向不在这里：它归 {@code StockEntryType} 表上的 {@code Direction} 管，是自建类型的一部分配置。
 * 程序自动开单那条路只看行上填了哪一端，不查表也不问枚举。
 */
@NullMarked
@Getter
public enum StockEntryTypeEnum implements BaseEnum {

  /** 领料出库：只填出库仓库，来源不是销售单（生产领料） */
  PRODUCTION_ISSUE("ProductionIssue", "领料出库"),

  /** 完工入库：只填入库仓库，来源是工单 */
  PRODUCTION_RECEIPT("ProductionReceipt", "完工入库"),

  /** 生产退料：只填入库仓库，来源是工单，料没用完从车间退回仓库（跟完工入库同形状，开单时须显式指定） */
  PRODUCTION_RETURN("ProductionReturn", "生产退料"),

  /** 调拨：出库仓库与入库仓库都填，一张单两头记账 */
  TRANSFER("Transfer", "调拨"),

  /** 销售出库：只填出库仓库，来源是销售单，货交给客户 */
  SALES_DELIVERY("SalesDelivery", "销售出库"),

  /** 销售退货：只填入库仓库，来源是销售单，货从客户退回来 */
  SALES_RETURN("SalesReturn", "销售退货"),

  /** 采购入库：只填入库仓库，来源不是工单（采购到货、其他入库） */
  PURCHASE_RECEIPT("PurchaseReceipt", "采购入库"),

  /** 采购退货：只填出库仓库，来源是采购单，货退回供应商 */
  PURCHASE_RETURN("PurchaseReturn", "采购退货");

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
