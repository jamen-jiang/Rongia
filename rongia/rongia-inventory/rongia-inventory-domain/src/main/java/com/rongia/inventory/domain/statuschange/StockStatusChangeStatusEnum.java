package com.rongia.inventory.domain.statuschange;

import com.rongia.shared.domain.enums.BaseEnum;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/**
 * 库存状态转换单状态
 *
 * <p>和库存记录一样三态，没有「已撤回」：过过账的转换单已经挪过状态，改状态改不回去。要纠正它，
 * 开一张反向的转换单（到状态 → 从状态），两笔加一块是零。
 */
@NullMarked
@Getter
public enum StockStatusChangeStatusEnum implements BaseEnum {

  /** 草稿：还没过账，一行状态没挪；用不着就作废 */
  DRAFT("Draft", "草稿"),

  /** 已过账：每行都完成了「从状态减、到状态加」，单据从此冻住 */
  POSTED("Posted", "已过账"),

  /** 已作废：草稿不用了撕掉，它一分账没动 */
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
  StockStatusChangeStatusEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }
}
