package com.rongia.basedata.domain.bankaccount;

import com.rongia.shared.domain.exception.DomainException;
import com.rongia.shared.domain.util.EnumUtils;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 银行账户工厂，负责创建银行账户 */
@NullMarked
public class BankAccountFactory {

  /** 银行账户仓储 */
  private final BankAccountRepository bankAccountRepository;

  /**
   * 构造器注入
   *
   * @param bankAccountRepository 银行账户仓储
   */
  public BankAccountFactory(BankAccountRepository bankAccountRepository) {
    this.bankAccountRepository = bankAccountRepository;
  }

  /**
   * 创建银行账户
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
   * @return 银行账户聚合
   * @throws DomainException 账号重复、或该归属已有默认账户时抛出
   */
  public BankAccount create(
      String partyType,
      long partyId,
      long bankId,
      @Nullable String bankBranch,
      String accountNumber,
      String accountName,
      @Nullable Long currencyId,
      boolean isDefault,
      boolean disabled) {
    checkUnique(partyType, partyId, accountNumber, isDefault, null);
    return new BankAccount(
        partyType,
        partyId,
        bankId,
        bankBranch,
        accountNumber,
        accountName,
        currencyId,
        isDefault,
        disabled);
  }

  /**
   * 校验账号与默认账户的唯一性
   *
   * @param partyType 归属类型编码
   * @param partyId 归属主数据Id
   * @param accountNumber 银行账号
   * @param isDefault 是否默认账户
   * @param id 排除的银行账户Id（可空，新增时传 null）
   * @throws DomainException 账号重复、或该归属已有默认账户时抛出
   */
  void checkUnique(
      String partyType,
      long partyId,
      String accountNumber,
      boolean isDefault,
      @Nullable Long id) {
    BankAccountPartyTypeEnum type = EnumUtils.fromCode(BankAccountPartyTypeEnum.class, partyType);
    String label = type == null ? partyType : type.getLabel();
    if (bankAccountRepository.isExistByPartyAndAccountNumberExcludeId(
        partyType, partyId, accountNumber, id)) {
      throw new DomainException(label + partyId + "已经有账号" + accountNumber + "了");
    }
    if (isDefault
        && bankAccountRepository.isExistDefaultByPartyExcludeId(partyType, partyId, id)) {
      throw new DomainException(label + partyId + "已经有默认银行账户了");
    }
  }
}
