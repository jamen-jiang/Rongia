package com.rongia.hr.domain.employee;

import com.rongia.shared.enums.BaseEnum;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/**
 * 用工类型枚举
 *
 * <p>取值参考 Odoo 18 {@code hr.employee.employee_type}（employee / worker / student / trainee /
 * contractor / freelance），砍到 4 个：
 *
 * <p>去掉 {@code worker}——Odoo 的 help 写着「Only Employees, Students and Trainee will have contract
 * history」，即 worker 指**不走劳动合同**的小时工/日结工；而中文语境的「工人」是签劳动合同的车间操作工，
 * 是岗位差异不是用工性质差异，那个维度交给 {@code Position}。保留会误导用户给车间工人勾「工人」，将来算合同
 * 时被当成不签合同的人。
 *
 * <p>去掉 {@code student}——在校勤工俭学在国内制造业基本不出现，真来了走 {@link #TRAINEE}。
 *
 * <p>Odoo 自己注明这个字段「只影响合同」，我们现在没有合同与薪酬，所以它此刻的作用是分类与筛选，等做劳动合同/
 * 薪酬时再让它决定哪些人需要签合同。
 */
@NullMarked
@Getter
public enum EmployeeTypeEnum implements BaseEnum {

  /** 正式工：签劳动合同的常规雇员 */
  REGULAR("Regular", "正式工"),

  /** 实习生：未毕业的实习人员 */
  TRAINEE("Trainee", "实习生"),

  /** 外包人员：劳务派遣、外包用工 */
  CONTRACTOR("Contractor", "外包人员"),

  /** 自由职业：顾问、兼职专家 */
  FREELANCE("Freelance", "自由职业");

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
  EmployeeTypeEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }
}
