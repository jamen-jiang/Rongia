package com.rongia.shared.domain.enums;

import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/**
 * 优先级（照 Odoo 的 PROCUREMENT_PRIORITIES：0 普通 / 1 紧急）
 *
 * <p>跨单据共用：工单、库存单据、采购单据上「这张单要不要插队」都用这两档，编码与 Odoo 一致。
 * 排产与预留按「紧急优先」排序，普通档不参与插队。
 */
@NullMarked
@Getter
public enum PriorityEnum implements BaseEnum {

  /** 普通 */
  NORMAL("0", "普通"),

  /** 紧急 */
  URGENT("1", "紧急");

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
  PriorityEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }
}
