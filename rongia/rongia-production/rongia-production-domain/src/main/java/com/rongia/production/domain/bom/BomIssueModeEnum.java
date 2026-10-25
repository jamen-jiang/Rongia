package com.rongia.production.domain.bom;

import com.rongia.shared.domain.enums.BaseEnum;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/**
 * 领料方式
 *
 * <p>照 Odoo 19 {@code mrp.bom.consumption}：允不允许实际领用量与 BOM 用量有出入。
 * {@code flexible} 随便领；{@code warning} 可以领但关单时提示差异；{@code strict} 不许超领/少领。
 */
@NullMarked
@Getter
public enum BomIssueModeEnum implements BaseEnum {

  /** 允许 */
  FLEXIBLE("Flexible", "允许"),

  /** 允许但提示 */
  WARNING("Warning", "允许但提示"),

  /** 禁止（只能按 BOM 用量领） */
  STRICT("Strict", "禁止");

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
  BomIssueModeEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }
}
