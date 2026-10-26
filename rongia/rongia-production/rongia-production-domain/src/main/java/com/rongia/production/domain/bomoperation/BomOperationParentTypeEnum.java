package com.rongia.production.domain.bomoperation;

import com.rongia.shared.domain.enums.BaseEnum;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/** 工序行的父级类型（照 ERPNext 的 BOM Operation.parenttype） */
@NullMarked
@Getter
public enum BomOperationParentTypeEnum implements BaseEnum {

  /** BOM：工序挂在配方上，下达工单时整份快照 */
  BOM("Bom", "配方"),

  /** 工艺路线：工序挂在路线上，可被多条 BOM 复用 */
  ROUTING("Routing", "工艺路线");

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
  BomOperationParentTypeEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }
}
