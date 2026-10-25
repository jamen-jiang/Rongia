package com.rongia.production.domain.workcenter;

import com.rongia.shared.domain.enums.BaseEnum;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/**
 * 工作中心实时状态
 *
 * <p>回答「这台中心此刻能不能干活」，三档照抄 Odoo 的 working_state：Normal/Blocked/In Progress。
 * 它由打卡记录自动推，我们暂由人填，等铺车间终端再改成自动。排产读它判断今天敢不敢接新活，不参与对账。
 */
@NullMarked
@Getter
public enum WorkCenterWorkingStateEnum implements BaseEnum {

  /** 正常：机器在转，可以接新活 */
  NORMAL("Normal", "正常"),

  /** 阻塞：机器卡住了（故障/缺料/等料），新工单点不开 */
  BLOCKED("Blocked", "阻塞"),

  /** 加工中：有工单正在这台中心上干 */
  DONE("In Progress", "加工中");

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
  WorkCenterWorkingStateEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }
}
