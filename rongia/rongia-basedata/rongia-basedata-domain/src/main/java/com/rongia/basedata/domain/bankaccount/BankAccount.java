package com.rongia.basedata.domain.bankaccount;

import com.rongia.basedata.domain.bank.Bank;
import com.rongia.shared.domain.aggregate.UpdateAggregateRoot;
import com.rongia.shared.domain.exception.DomainException;
import com.rongia.shared.domain.util.EnumUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Nationalized;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 银行账户（通用）
 *
 * <p>归属用「类型 + Id」表示（{@link BankAccountPartyTypeEnum}），所以供应商、客户、本公司的账户是同一张表；
 * 开户银行用 {@link Bank} 的 Id 引用，不是文本。对应 ERPNext 的 {@code Bank Account}
 * （{@code party_type} + {@code party} + {@code bank}）、Odoo 的 {@code res.partner.bank}
 * （{@code partner_id} + {@code bank_id}）、SAP 的 {@code LFBK}（银行代码 + 账号）。
 *
 * <p>两条唯一性都靠过滤唯一索引兜底，JPA 注解表达不了：
 * 同一个归属下账号不重复（{@code (PartyType, PartyId, AccountNumber)}，{@code WHERE IsDeleted=0}）、
 * 同一个归属下只有一个默认账户（{@code (PartyType, PartyId)}，{@code WHERE IsDeleted=0 AND IsDefault=1}）。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "BankAccount",
    indexes = {
      @Index(name = "IX_BankAccount_Party", columnList = "PartyType,PartyId"),
      @Index(name = "IX_BankAccount_BankId", columnList = "BankId")
      // 注意：(PartyType, PartyId, AccountNumber) 与 (PartyType, PartyId) WHERE IsDefault=1
      // 的过滤唯一索引带 WHERE IsDeleted=0，JPA 不支持，用 Flyway 创建
    })
public class BankAccount extends UpdateAggregateRoot {

  /** 归属类型编码，取值见 {@link BankAccountPartyTypeEnum} */
  @Column(name = "PartyType", nullable = false, length = 50)
  private String partyType;

  /** 归属主数据Id（供应商Id / 客户Id / 公司Id） */
  @Column(name = "PartyId", nullable = false)
  private Long partyId;

  /** 开户银行Id */
  @Column(name = "BankId", nullable = false)
  private Long bankId;

  /** 开户支行（可空） */
  @Nationalized
  @Column(name = "BankBranch", length = 200)
  @Nullable
  private String bankBranch;

  /** 银行账号 */
  @Nationalized
  @Column(name = "AccountNumber", nullable = false, length = 50)
  private String accountNumber;

  /** 账户名称（户名） */
  @Nationalized
  @Column(name = "AccountName", nullable = false, length = 100)
  private String accountName;

  /** 账户币种Id（可空，空表示与本位币相同） */
  @Column(name = "CurrencyId")
  @Nullable
  private Long currencyId;

  /** 是否默认账户 */
  @Column(name = "IsDefault", nullable = false)
  private Boolean isDefault;

  /** 是否禁用 */
  @Column(name = "Disabled", nullable = false)
  private Boolean disabled;

  /**
   * 构造器
   *
   * @param partyType 归属类型编码
   * @param partyId 归属主数据Id
   * @param bankId 开户银行Id
   * @param bankBranch 开户支行（可空）
   * @param accountNumber 银行账号
   * @param accountName 账户名称（户名）
   * @param currencyId 账户币种Id（可空）
   * @param isDefault 是否默认账户
   * @param disabled 是否禁用
   */
  BankAccount(
      String partyType,
      long partyId,
      long bankId,
      @Nullable String bankBranch,
      String accountNumber,
      String accountName,
      @Nullable Long currencyId,
      boolean isDefault,
      boolean disabled) {
    apply(
        partyType, partyId, bankId, bankBranch, accountNumber, accountName, currencyId, isDefault,
        disabled);
  }

  /**
   * 修改银行账户
   *
   * <p>归属（类型 + Id）是这一行的身份，不提供修改：账户要换到另一个供应商，就当新账户建。
   *
   * @param bankId 开户银行Id
   * @param bankBranch 开户支行（可空）
   * @param accountNumber 银行账号
   * @param accountName 账户名称（户名）
   * @param currencyId 账户币种Id（可空）
   * @param isDefault 是否默认账户
   * @param disabled 是否禁用
   */
  void modify(
      long bankId,
      @Nullable String bankBranch,
      String accountNumber,
      String accountName,
      @Nullable Long currencyId,
      boolean isDefault,
      boolean disabled) {
    apply(
        this.partyType, this.partyId, bankId, bankBranch, accountNumber, accountName, currencyId,
        isDefault, disabled);
  }

  // ==================== 私有：校验与赋值 ====================

  /**
   * 统一的校验与赋值入口（构造器与修改共用）
   *
   * @param partyType 归属类型编码
   * @param partyId 归属主数据Id
   * @param bankId 开户银行Id
   * @param bankBranch 开户支行（可空）
   * @param accountNumber 银行账号
   * @param accountName 账户名称（户名）
   * @param currencyId 账户币种Id（可空）
   * @param isDefault 是否默认账户
   * @param disabled 是否禁用
   * @throws DomainException 归属类型有误、归属/银行/账号/户名为空、或币种Id非法时抛出
   */
  private void apply(
      String partyType,
      long partyId,
      long bankId,
      @Nullable String bankBranch,
      String accountNumber,
      String accountName,
      @Nullable Long currencyId,
      boolean isDefault,
      boolean disabled) {
    String trimmedAccountName = StringUtils.trimToNull(accountName);
    String trimmedAccountNumber = StringUtils.trimToNull(accountNumber);
    this.partyType = EnumUtils.getCode(BankAccountPartyTypeEnum.class, partyType, "银行账户的归属类型");
    if (partyId <= 0) {
      throw new DomainException("银行账户必须指定归属对象");
    }
    if (trimmedAccountName == null) {
      throw new DomainException("银行账户的账户名称不能为空");
    }
    if (bankId <= 0) {
      throw new DomainException("银行账户的开户银行不能为空");
    }
    if (trimmedAccountNumber == null) {
      throw new DomainException("银行账号不能为空");
    }
    if (currencyId != null && currencyId <= 0) {
      throw new DomainException("银行账户的币种不能为空");
    }

    this.partyId = partyId;
    this.bankId = bankId;
    this.bankBranch = StringUtils.trimToNull(bankBranch);
    this.accountNumber = trimmedAccountNumber;
    this.accountName = trimmedAccountName;
    this.currencyId = currencyId;
    this.isDefault = isDefault;
    this.disabled = disabled;
  }
}
