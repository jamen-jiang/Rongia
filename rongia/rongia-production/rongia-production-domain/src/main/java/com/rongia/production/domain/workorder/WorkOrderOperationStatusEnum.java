package com.rongia.production.domain.workorder;

import com.rongia.shared.domain.enums.BaseEnum;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/** 工单工序状态 */
@NullMarked
@Getter
public enum WorkOrderOperationStatusEnum implements BaseEnum {

  /** 未开始：前置工序尚未完工 */
  PENDING("Pending", "未开始"),

  /** 可开工：前置工序已完工（首道工序下达即为可开工） */
  READY("Ready", "可开工"),

  /** 加工中：自制工序为开工，外协工序为已发出 */
  IN_PROGRESS("InProgress", "加工中"),

  /** 已完工 */
  COMPLETED("Completed", "已完工"),

  /** 已跳过：仅可选工序可跳过 */
  SKIPPED("Skipped", "已跳过");

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
  WorkOrderOperationStatusEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }
}
