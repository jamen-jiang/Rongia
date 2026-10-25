package com.rongia.basedata.domain.bankaccount;

import com.rongia.basedata.domain.bank.BankRepository;
import com.rongia.basedata.domain.currency.CurrencyRepository;
import com.rongia.basedata.domain.supplier.SupplierRepository;
import com.rongia.shared.domain.exception.DomainException;
import com.rongia.shared.domain.util.EnumUtils;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 银行账户领域服务，负责跨聚合编排 */
@NullMarked
public class BankAccountDomainService {

  /** 银行账户工厂 */
  private final BankAccountFactory bankAccountFactory;

  /** 银行账户仓储 */
  private final BankAccountRepository bankAccountRepository;

  /** 供应商仓储：归属是供应商时要确认它存在 */
  private final SupplierRepository supplierRepository;

  /** 银行仓储：开户银行必须是已维护的银行 */
  private final BankRepository bankRepository;

  /** 币种仓储：账户币种必须存在 */
  private final CurrencyRepository currencyRepository;

  /**
   * 构造器注入
   *
   * @param bankAccountFactory 银行账户工厂
   * @param bankAccountRepository 银行账户仓储
   * @param supplierRepository 供应商仓储
   * @param bankRepository 银行仓储
   * @param currencyRepository 币种仓储
   */
  public BankAccountDomainService(
      BankAccountFactory bankAccountFactory,
      BankAccountRepository bankAccountRepository,
      SupplierRepository supplierRepository,
      BankRepository bankRepository,
      CurrencyRepository currencyRepository) {
    this.bankAccountFactory = bankAccountFactory;
    this.bankAccountRepository = bankAccountRepository;
    this.supplierRepository = supplierRepository;
    this.bankRepository = bankRepository;
    this.currencyRepository = currencyRepository;
  }

  /**
   * 新建银行账户
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
   * @throws DomainException 归属对象、银行或币种不存在、账号重复、或该归属已有默认账户时抛出
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
    requireParty(partyType, partyId);
    requireBank(bankId);
    requireCurrency(currencyId);
    return bankAccountFactory.create(
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
   * 导入生成：同一归属下账号存在则修改，不存在则创建
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
   */
  public BankAccount importGenerate(
      String partyType,
      long partyId,
      long bankId,
      @Nullable String bankBranch,
      String accountNumber,
      String accountName,
      @Nullable Long currencyId,
      boolean isDefault,
      boolean disabled) {
    // 按归属 + 账号查银行账户
    BankAccount model =
        bankAccountRepository.getByPartyAndAccountNumber(partyType, partyId, accountNumber);

    // 不存在则创建，存在则修改
    if (model == null) {
      model =
          create(
              partyType,
              partyId,
              bankId,
              bankBranch,
              accountNumber,
              accountName,
              currencyId,
              isDefault,
              disabled);
    } else {
      modify(
          model,
          bankId,
          bankBranch,
          accountNumber,
          accountName,
          currencyId,
          isDefault,
          disabled);
    }

    return model;
  }

  /**
   * 修改银行账户
   *
   * @param id 银行账户Id
   * @param bankId 开户银行Id
   * @param bankBranch 开户支行（可空）
   * @param accountNumber 银行账号
   * @param accountName 账户名称（户名）
   * @param currencyId 账户币种Id（可空）
   * @param isDefault 是否默认账户
   * @param disabled 是否禁用
   * @return 银行账户聚合
   * @throws DomainException 银行账户不存在时抛出
   */
  public BankAccount modify(
      long id,
      long bankId,
      @Nullable String bankBranch,
      String accountNumber,
      String accountName,
      @Nullable Long currencyId,
      boolean isDefault,
      boolean disabled) {
    BankAccount model = bankAccountRepository.get(id);
    if (model == null) {
      throw new DomainException("银行账户" + id + "不存在!");
    }
    modify(model, bankId, bankBranch, accountNumber, accountName, currencyId, isDefault, disabled);
    return model;
  }

  /**
   * 修改银行账户（使用已有聚合）
   *
   * @param model 银行账户聚合
   * @param bankId 开户银行Id
   * @param bankBranch 开户支行（可空）
   * @param accountNumber 银行账号
   * @param accountName 账户名称（户名）
   * @param currencyId 账户币种Id（可空）
   * @param isDefault 是否默认账户
   * @param disabled 是否禁用
   * @throws DomainException 银行或币种不存在、账号重复、或该归属已有其它默认账户时抛出
   */
  public void modify(
      BankAccount model,
      long bankId,
      @Nullable String bankBranch,
      String accountNumber,
      String accountName,
      @Nullable Long currencyId,
      boolean isDefault,
      boolean disabled) {
    requireBank(bankId);
    requireCurrency(currencyId);
    bankAccountFactory.checkUnique(
        model.getPartyType(), model.getPartyId(), accountNumber, isDefault, model.getId());
    model.modify(bankId, bankBranch, accountNumber, accountName, currencyId, isDefault, disabled);
  }

  // ==================== 私有：校验 ====================

  /**
   * 校验归属类型合法、归属对象存在
   *
   * <p>只有供应商的存在性在这一层查得到：公司主数据在 {@code rongia-system}，basedata 不引入横向模块依赖，
   * 所以公司（{@code COMPANY}）的存在性由调用方（应用层）保证。以后客户主数据进来了同理。
   *
   * @param partyType 归属类型编码
   * @param partyId 归属主数据Id
   * @throws DomainException 归属类型有误、或供应商不存在时抛出
   */
  private void requireParty(String partyType, long partyId) {
    BankAccountPartyTypeEnum type =
        EnumUtils.fromCode(BankAccountPartyTypeEnum.class, partyType);
    if (type == null) {
      throw new DomainException("银行账户的归属类型" + partyType + "有误!");
    }
    if (type == BankAccountPartyTypeEnum.SUPPLIER && supplierRepository.get(partyId) == null) {
      throw new DomainException("供应商" + partyId + "不存在!");
    }
  }

  /**
   * 校验银行存在
   *
   * @param bankId 银行Id
   * @throws DomainException 银行不存在时抛出
   */
  private void requireBank(long bankId) {
    if (bankRepository.get(bankId) == null) {
      throw new DomainException("银行" + bankId + "不存在!");
    }
  }

  /**
   * 校验币种存在（可空）
   *
   * @param currencyId 币种Id（可空）
   * @throws DomainException 币种不存在时抛出
   */
  private void requireCurrency(@Nullable Long currencyId) {
    if (currencyId != null && currencyRepository.get(currencyId) == null) {
      throw new DomainException("币种" + currencyId + "不存在!");
    }
  }
}
