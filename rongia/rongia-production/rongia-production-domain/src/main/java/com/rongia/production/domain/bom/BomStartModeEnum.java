package com.rongia.production.domain.bom;

import com.rongia.shared.domain.enums.BaseEnum;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/**
 * 开工方式
 *
 * <p>照 Odoo 19 {@code mrp.bom.ready_to_produce}：这张 BOM 的工单要等到什么时候才能开工。
 * {@code all_available} 所有组件齐了才开；{@code asap} 第一道工序的组件齐了就开，后续工序边做边补料。
 */
@NullMarked
@Getter
public enum BomStartModeEnum implements BaseEnum {

  /** 所有组件齐了才开工 */
  ALL_AVAILABLE("AllAvailable", "所有组件齐套"),

  /** 首道工序的组件齐了就开工 */
  ASAP("Asap", "首道工序齐套");

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
  BomStartModeEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }
}
