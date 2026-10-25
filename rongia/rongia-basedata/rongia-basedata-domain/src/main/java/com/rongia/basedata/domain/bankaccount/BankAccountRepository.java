package com.rongia.basedata.domain.bankaccount;

import com.rongia.shared.domain.repository.Repository;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 银行账户仓储接口 */
@NullMarked
public interface BankAccountRepository extends Repository<BankAccount> {

  /**
   * 按归属查询全部银行账户
   *
   * @param partyType 归属类型编码
   * @param partyId 归属主数据Id
   * @return 银行账户列表
   */
  List<BankAccount> getListByParty(String partyType, long partyId);

  /**
   * 按归属 + 银行账号查询
   *
   * @param partyType 归属类型编码
   * @param partyId 归属主数据Id
   * @param accountNumber 银行账号
   * @return 银行账户（可空），不存在返回 null
   */
  @Nullable BankAccount getByPartyAndAccountNumber(
      String partyType, long partyId, String accountNumber);

  /**
   * 判断同一归属下银行账号是否已存在
   *
   * @param partyType 归属类型编码
   * @param partyId 归属主数据Id
   * @param accountNumber 银行账号
   * @param id 排除的银行账户Id（可空，新增时传 null）
   * @return true 表示已存在
   */
  boolean isExistByPartyAndAccountNumberExcludeId(
      String partyType, long partyId, String accountNumber, @Nullable Long id);

  /**
   * 判断同一归属下银行账号是否已存在（新增时校验）
   *
   * @param partyType 归属类型编码
   * @param partyId 归属主数据Id
   * @param accountNumber 银行账号
   * @return true 表示已存在
   */
  default boolean isExistByPartyAndAccountNumberExcludeId(
      String partyType, long partyId, String accountNumber) {
    return isExistByPartyAndAccountNumberExcludeId(partyType, partyId, accountNumber, null);
  }

  /**
   * 判断该归属是否已有默认账户
   *
   * @param partyType 归属类型编码
   * @param partyId 归属主数据Id
   * @param id 排除的银行账户Id（可空，新增时传 null）
   * @return true 表示已存在
   */
  boolean isExistDefaultByPartyExcludeId(String partyType, long partyId, @Nullable Long id);

  /**
   * 判断该归属是否已有默认账户（新增时校验）
   *
   * @param partyType 归属类型编码
   * @param partyId 归属主数据Id
   * @return true 表示已存在
   */
  default boolean isExistDefaultByPartyExcludeId(String partyType, long partyId) {
    return isExistDefaultByPartyExcludeId(partyType, partyId, null);
  }
}
