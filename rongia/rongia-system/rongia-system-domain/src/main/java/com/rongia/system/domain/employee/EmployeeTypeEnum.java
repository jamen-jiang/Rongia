package com.rongia.system.domain.employee;

import com.rongia.shared.domain.enums.BaseEnum;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/**
 * 用工类型枚举
 *
 * <p>取值照 Odoo {@code hr.employee.employee_type}
 * （employee / worker / student / trainee / contractor / freelance），Odoo 自己注明它「只影响合同」——
 * 我们现在没有合同与薪酬，所以它此刻的作用是分类与筛选，等做劳动合同/薪酬时再让它决定哪些人需要签合同。
 */
@NullMarked
@Getter
public enum EmployeeTypeEnum implements BaseEnum {

  /** 员工 */
  EMPLOYEE("Employee", "员工"),

  /** 工人 */
  WORKER("Worker", "工人"),

  /** 学生 */
  STUDENT("Student", "学生"),

  /** 实习生 */
  TRAINEE("Trainee", "实习生"),

  /** 外包人员 */
  CONTRACTOR("Contractor", "外包人员"),

  /** 自由职业 */
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