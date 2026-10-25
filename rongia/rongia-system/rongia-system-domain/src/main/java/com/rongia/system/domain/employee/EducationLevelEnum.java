package com.rongia.system.domain.employee;

import com.rongia.shared.domain.enums.BaseEnum;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/**
 * 最高学历枚举
 *
 * <p>Odoo {@code hr.employee.certificate} 是 graduate / bachelor / master / doctor / other 五档，
 * 但 "Graduate" 在国内口径里分不清高中和专科。这里保留四个共有的英文编码，再拆成
 * **高中及以下 + 专科**两档——工厂的员工主体是工人，没有「高中及以下」会被迫全选「其他」。
 */
@NullMarked
@Getter
public enum EducationLevelEnum implements BaseEnum {

  /** 高中及以下 */
  HIGH_SCHOOL_OR_BELOW("HighSchoolOrBelow", "高中及以下"),

  /** 专科 */
  COLLEGE("College", "专科"),

  /** 本科 */
  BACHELOR("Bachelor", "本科"),

  /** 硕士 */
  MASTER("Master", "硕士"),

  /** 博士 */
  DOCTOR("Doctor", "博士"),

  /** 其他 */
  OTHER("Other", "其他");

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
  EducationLevelEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }
}
