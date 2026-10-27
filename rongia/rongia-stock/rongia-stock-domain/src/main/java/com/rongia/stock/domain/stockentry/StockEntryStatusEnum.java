package com.rongia.stock.domain.stockentry;

import com.rongia.shared.domain.enums.BaseEnum;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/**
 * 库存单据状态
 *
 * <p>三态：草稿、完成、已取消。草稿不要了转已取消，留一行痕迹；已完成的原单永远停在完成，
 * 被冲掉这件事由另开的一张红字单表达，原单上不留痕迹——改状态改回头，库存那边已经动过的数就没人认领了。
 * （口径照 Odoo 的 stock.picking：done 表示货动了、账记了。）
 */
@NullMarked
@Getter
public enum StockEntryStatusEnum implements BaseEnum {

  /** 草稿：还没过账，库存一分没动；过不去账就先挂着 */
  DRAFT("Draft", "草稿"),

  /** 完成：库存已按行加减完，单据从此冻住，要撤销只能红冲 */
  DONE("Done", "完成"),

  /** 已取消：草稿作废后的终态，库存一分没动，只是留下「这张单作废了」的痕迹 */
  CANCELLED("Cancelled", "已取消"),
;

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
  StockEntryStatusEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }
}
