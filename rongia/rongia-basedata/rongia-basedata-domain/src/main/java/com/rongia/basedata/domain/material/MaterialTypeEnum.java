package com.rongia.basedata.domain.material;

import com.rongia.shared.domain.enums.BaseEnum;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/**
 * 物料类型枚举
 *
 * <p>Odoo 19 的 {@code product.template.type} 只有三档：{@code consu}（货物）/ {@code service}（服务）/
 * {@code combo}（套餐），「这张货管不管库存」由 stock 模块的 {@code is_storable} 单独表达。我们合成一列三档：
 * <b>库存料</b> = {@code consu} + {@code is_storable=true}，
 * <b>消耗品</b> = {@code consu} + {@code is_storable=false}，<b>服务</b> = {@code service}；
 * 没有套餐（我们的 BOM 不在产品主档上表达）。
 *
 * <p>「自制还是外购」不在这一列：Odoo 用产品上的路线（采购、制造、分包）表达，我们对应的是「有没有工艺路线」和
 * 「有没有采购价格」，不额外占一列。
 *
 * <p><b>当前没有消费方</b>：除了建/改物料时校验取值，还没有任何流程按它分支（入库、BOM、领料都还没读）。
 * 等第一张要判断「管不管库存」的单据出现时再让它起作用。
 */
@NullMarked
@Getter
public enum MaterialTypeEnum implements BaseEnum {

  /** 库存料：进出库存账，可上 BOM、可领料 */
  STOCK("Stock", "库存料"),

  /** 消耗品：劳保、办公用品这类，买了直接领用，不进库存账 */
  CONSUMABLE("Consumable", "消耗品"),

  /** 服务：运输、检测、加工费这类非实物 */
  SERVICE("Service", "服务");

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
  MaterialTypeEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }
}
