package com.rongia.production.domain.productionreturn;

import com.rongia.shared.domain.enums.BaseEnum;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/** 生产退料单状态 */
@NullMarked
@Getter
public enum ProductionReturnStatusEnum implements BaseEnum {

  /** 草稿：还在申请，既没动库存也没动工单用料 */
  DRAFT("Draft", "草稿"),

  /** 已退料：核定数量真回到仓库了，单据落定 */
  POSTED("Posted", "已退料");

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
  ProductionReturnStatusEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }
}
