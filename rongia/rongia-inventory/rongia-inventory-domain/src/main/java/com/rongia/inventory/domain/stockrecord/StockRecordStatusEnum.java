package com.rongia.inventory.domain.stockrecord;

import com.rongia.shared.domain.enums.BaseEnum;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/**
 * 库存记录状态
 *
 * <p>只有三态，没有「已撤回」：过过账的原单永远停在已过账，被冲掉这件事由另开的一张红字单表达，
 * 原单上不留痕迹。改状态改回头，库存那边已经动过的数就没人认领了。
 */
@NullMarked
@Getter
public enum StockRecordStatusEnum implements BaseEnum {

  /** 草稿：还没过账，库存一分没动；用不着就作废，过不去账就先挂着 */
  DRAFT("Draft", "草稿"),

  /** 已过账：库存已按行加减完，单据从此冻住，要撤销只能红冲 */
  POSTED("Posted", "已过账"),

  /** 已作废：草稿不用了撕掉，它本来就没动过账，没有要冲的东西 */
  VOIDED("Voided", "已作废");

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
  StockRecordStatusEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }
}
