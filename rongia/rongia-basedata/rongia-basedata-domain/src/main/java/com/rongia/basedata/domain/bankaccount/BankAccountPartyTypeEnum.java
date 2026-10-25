package com.rongia.basedata.domain.bankaccount;

import com.rongia.shared.domain.enums.BaseEnum;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/**
 * 银行账户的归属类型
 *
 * <p>银行账户是通用对象：供应商、客户、本公司都可能有账户，所以用「类型 + 主数据Id」指认归属，
 * 而不是给每种对象各建一张表。ERPNext 的 {@code Bank Account} 就是
 * {@code party_type} + {@code party}（Dynamic Link）+ {@code is_company_account}，Odoo 是
 * {@code res.partner.bank.partner_id}（客户和供应商都是 partner）。
 *
 * <p>已接入三种：供应商 {@code SUPPLIER}、本公司 {@code COMPANY}、员工 {@code EMPLOYEE}（工资卡）；
 * 客户主数据接入时加值即可，表结构不用动。
 */
@NullMarked
@Getter
public enum BankAccountPartyTypeEnum implements BaseEnum {

  /** 供应商 */
  SUPPLIER("Supplier", "供应商"),

  /** 本公司 */
  COMPANY("Company", "公司"),

  /** 员工 */
  EMPLOYEE("Employee", "员工");

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
  BankAccountPartyTypeEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }
}
