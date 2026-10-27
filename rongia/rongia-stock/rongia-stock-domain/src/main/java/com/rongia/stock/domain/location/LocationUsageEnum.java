package com.rongia.stock.domain.location;

import com.rongia.shared.domain.enums.BaseEnum;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/**
 * 库位用途（照 Odoo 的 {@code stock.location.usage}）
 *
 * <p>它决定一笔移动是入库还是出库：从 {@code Supplier} 进 {@code Internal} 是采购入库，从
 * {@code Internal} 出到 {@code Customer} 是销售出库。方向不写在单据上，写在两端位置的用途上。
 */
@NullMarked
@Getter
public enum LocationUsageEnum implements BaseEnum {

  /** 内部：仓库里真实存货的库位 */
  INTERNAL("Internal", "内部"),

  /** 供应商：虚拟位置，采购入库的起点 */
  SUPPLIER("Supplier", "供应商"),

  /** 客户：虚拟位置，销售出库的终点 */
  CUSTOMER("Customer", "客户"),

  /** 生产：虚拟位置，领料与完工入库的一端 */
  PRODUCTION("Production", "生产"),

  /** 库存损失：虚拟位置，盘盈盘亏的一端 */
  INVENTORY("Inventory", "库存损失"),

  /** 在途：虚拟位置，跨仓调拨的中间态 */
  TRANSIT("Transit", "在途"),

  /** 结构节点：只用于给库位分组，不能直接存货 */
  VIEW("View", "结构节点");

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
  LocationUsageEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }
}
