package com.rongia.system.domain.employeecontract;

import com.rongia.shared.domain.enums.BaseEnum;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/**
 * 劳动合同期限类型枚举
 *
 * <p>取值照《劳动合同法》第十二条的三类：固定期限 / 无固定期限 / 以完成一定工作任务为期限。
 * 这是**法的分类**，不是配置项，所以做成枚举而不是主数据（Odoo 用 {@code hr.contract.type}
 * 主数据，因为它的取值随国家本地化变化，例如比利时的 CDI / CDD / PFI）。
 *
 * <p>与员工上的 {@code EmployeeTypeEnum} 不是一回事：那个说的是员工身份（员工/工人/实习生/
 * 外包/自由职业），这个说的是合同期限怎么算。两者可以任意组合。
 */
@NullMarked
@Getter
public enum EmployeeContractTypeEnum implements BaseEnum {

  /** 固定期限：必须填结束日期 */
  FIXED_TERM("FixedTerm", "固定期限"),

  /** 无固定期限：不能填结束日期 */
  OPEN_ENDED("OpenEnded", "无固定期限"),

  /** 以完成一定工作任务为期限：结束日期是可预计的完工日，可空 */
  TASK_BASED("TaskBased", "以完成一定工作任务为期限");

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
  EmployeeContractTypeEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }
}
