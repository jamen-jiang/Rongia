package com.rongia.stock.domain.stockentry;

import com.rongia.shared.domain.enums.BaseEnum;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/**
 * 库存单据状态
 *
 * <p>只有两态：草稿、已提交。草稿不要了直接删记录，不设「已取消」终态；已提交的原单永远停在已提交，
 * 被冲掉这件事由另开的一张红字单表达，原单上不留痕迹——改状态改回头，库存那边已经动过的数就没人认领了。
 */
@NullMarked
@Getter
public enum StockEntryStatusEnum implements BaseEnum {

  /** 草稿：还没提交，库存一分没动；用不着就作废，过不去账就先挂着 */
  DRAFT("Draft", "草稿"),

  /** 已提交：库存已按行加减完，单据从此冻住，要撤销只能红冲 */
  SUBMITTED("Submitted", "已提交"),
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
