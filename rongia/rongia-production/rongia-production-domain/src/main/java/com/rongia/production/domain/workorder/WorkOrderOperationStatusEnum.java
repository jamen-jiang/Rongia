package com.rongia.production.domain.workorder;

import com.rongia.shared.domain.enums.BaseEnum;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/**
 * 工单工序状态
 *
 * <p>三档照 ERPNext 的 Work Order Operation.status：Pending/Work in Progress/Completed。
 * 它是「干到哪一步」的结论，不含「能不能开工」——能不能开工由前置工序是否完工现算，不落状态，
 * 所以没有「可开工」这一档，也没有「已跳过」：跳过的工序直接算完工、产出为 0。
 */
@NullMarked
@Getter
public enum WorkOrderOperationStatusEnum implements BaseEnum {

  /** 未开始：还没报过工 */
  PENDING("Pending", "未开始"),

  /** 加工中：已开工、尚未完工 */
  IN_PROGRESS("Work in Progress", "加工中"),

  /** 已完工 */
  COMPLETED("Completed", "已完工");

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
