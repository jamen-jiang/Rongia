package com.rongia.production.domain.workorder;

import com.rongia.shared.domain.enums.BaseEnum;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/** 工单状态 */
@NullMarked
@Getter
public enum WorkOrderStatusEnum implements BaseEnum {

  /** 草稿：可改数量、可换路线 */
  DRAFT("Draft", "草稿"),

  /** 已下达：路线已快照成工序，不可再改 */
  RELEASED("Released", "已下达"),

  /** 生产中：至少一道工序已开工 */
  IN_PROGRESS("InProgress", "生产中"),

  /** 已完工：每道工序都收了尾（已完工或已跳过），或产出与报废凑够计划数量后手工结单 */
  COMPLETED("Completed", "已完工"),

  /** 已关闭：财务/库存已结案，不再允许变更 */
  CLOSED("Closed", "已关闭"),

  /** 已取消 */
  CANCELLED("Cancelled", "已取消");

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
  WorkOrderStatusEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }
}
