package com.rongia.production.domain.bom;

import com.rongia.shared.domain.enums.BaseEnum;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/**
 * BOM 类型
 *
 * <p>照 Odoo 19 {@code mrp.bom.type} 的两档：{@code normal} 正常制造——按这张 BOM 下工单；
 * {@code phantom} 套件——本身就是一组子件，不单独下工单，需求直接展开到下一层。
 */
@NullMarked
@Getter
public enum BomTypeEnum implements BaseEnum {

  /** 正常制造 */
  NORMAL("Normal", "制造"),

  /** 套件（幽灵件，不下工单直接展开） */
  PHANTOM("Phantom", "套件");

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
  BomTypeEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }
}
