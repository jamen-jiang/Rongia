package com.rongia.basedata.domain.bank;

import com.rongia.shared.domain.exception.DomainException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 银行工厂，负责创建银行 */
@NullMarked
public class BankFactory {

  /** 银行仓储 */
  private final BankRepository bankRepository;

  /**
   * 构造器注入
   *
   * @param bankRepository 银行仓储
   */
  public BankFactory(BankRepository bankRepository) {
    this.bankRepository = bankRepository;
  }

  /**
   * 创建银行
   *
   * @param code 银行代码
   * @param name 银行名称
   * @param disabled 是否禁用
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   * @return 银行聚合
   * @throws DomainException 银行代码已存在时抛出
   */
  public Bank create(
      String code, String name, boolean disabled, @Nullable Integer sequence, @Nullable String remark) {
    // 校验银行代码唯一
    if (bankRepository.isExistByCodeExcludeId(code)) {
      throw new DomainException("银行代码" + code + "已存在!");
    }
    return new Bank(code, name, disabled, sequence, remark);
  }
}
