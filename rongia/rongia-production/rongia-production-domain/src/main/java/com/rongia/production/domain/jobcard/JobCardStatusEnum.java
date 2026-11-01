package com.rongia.production.domain.jobcard;

import com.rongia.shared.domain.enums.BaseEnum;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/**
 * 工卡状态
 *
 * <p>照 ERPNext 的作业卡：没提交前随便加删报工行，提交后整张卡锁死，要撤只能整卡取消。
 */
@NullMarked
@Getter
public enum JobCardStatusEnum implements BaseEnum {

  /** 未开始：还没报过工，报工行可加可删 */
  PENDING("Pending", "未开始"),

  /** 加工中：已经报过工、还没提交，报工行照样可加可删 */
  WORK_IN_PROGRESS("WorkInProgress", "加工中"),

  /** 已完成：已提交，卡锁死，不能再改 */
  COMPLETED("Completed", "已完成"),

  /** 已取消：整张卡作废 */
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
  JobCardStatusEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }
}
