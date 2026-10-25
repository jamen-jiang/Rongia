package com.rongia.system.domain.employee;

import com.rongia.shared.domain.enums.BaseEnum;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/**
 * 员工状态枚举
 *
 * <p>只留两档：**在职 / 离职**。ERPNext {@code Employee.status} 有四档
 * （Active / Inactive / Suspended / Left），但「停用」「停职」这两档我们没有任何流程去区分——
 * 金蝶 ERP 的员工档案更只有一个「禁用」开关（在职/离职在 s-HR 里）。所以这里按事实收敛：
 * 离职＝不在公司了，由离职日期与原因承接；其余情况都是在职。
 */
@NullMarked
@Getter
public enum EmployeeStatusEnum implements BaseEnum {

  /** 在职 */
  ACTIVE("Active", "在职"),

  /** 离职 */
  LEFT("Left", "离职");

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
  EmployeeStatusEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }
}