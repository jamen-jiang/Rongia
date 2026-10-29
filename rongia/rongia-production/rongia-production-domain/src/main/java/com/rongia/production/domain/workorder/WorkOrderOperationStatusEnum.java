package com.rongia.production.domain.workorder;

import com.rongia.shared.domain.enums.BaseEnum;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/**
 * 工单工序状态
 *
 * <p>三档照 ERPNext 的 Work Order Operation.status：Pending/Work in Progress/Completed，编码按本项目的写法
 * 去掉空格（WorkInProgress）。状态不是人点的，由累计良品对可投入量算出来：没报是未开始、报了一些是加工中、
 * 报满就是已完工。
 *
 * <p>它是「干到哪一步」的结论，不含「能不能开工」——能不能动手由可投入量卡着，不落状态，
 * 所以没有「可开工」这一档，也没有「已跳过」：跳过的工序直接报满、产出为 0。
 */
@NullMarked
@Getter
public enum WorkOrderOperationStatusEnum implements BaseEnum {

  /** 未开始：还没报过工 */
  PENDING("Pending", "未开始"),

  /** 加工中：已开工、尚未完工 */
  WORK_IN_PROGRESS("WorkInProgress", "加工中"),

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
