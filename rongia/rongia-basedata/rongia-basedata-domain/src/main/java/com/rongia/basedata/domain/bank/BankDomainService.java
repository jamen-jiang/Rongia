package com.rongia.basedata.domain.bank;

import com.rongia.shared.domain.exception.DomainException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 银行领域服务 */
@NullMarked
public class BankDomainService {

  /** 银行工厂 */
  private final BankFactory bankFactory;

  /** 银行仓储 */
  private final BankRepository bankRepository;

  /**
   * 构造器注入
   *
   * @param bankFactory 银行工厂
   * @param bankRepository 银行仓储
   */
  public BankDomainService(BankFactory bankFactory, BankRepository bankRepository) {
    this.bankFactory = bankFactory;
    this.bankRepository = bankRepository;
  }

  /**
   * 导入生成：银行存在则修改，不存在则创建
   *
   * @param code 银行代码
   * @param name 银行名称
   * @param disabled 是否禁用
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   * @return 银行聚合
   */
  public Bank importGenerate(
      String code, String name, boolean disabled, @Nullable Integer sequence, @Nullable String remark) {
    // 按代码查银行
    Bank model = bankRepository.getByCode(code);

    // 不存在则创建，存在则修改
    if (model == null) {
      model = bankFactory.create(code, name, disabled, sequence, remark);
    } else {
      modify(model, code, name, disabled, sequence, remark);
    }

    return model;
  }

  /**
   * 修改银行
   *
   * @param id 银行Id
   * @param code 银行代码
   * @param name 银行名称
   * @param disabled 是否禁用
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   * @return 银行聚合
   * @throws DomainException 银行不存在时抛出
   */
  public Bank modify(
      long id,
      String code,
      String name,
      boolean disabled,
      @Nullable Integer sequence,
      @Nullable String remark) {
    Bank model = bankRepository.get(id);
    if (model == null) {
      throw new DomainException("银行" + id + "不存在!");
    }
    modify(model, code, name, disabled, sequence, remark);
    return model;
  }

  /**
   * 修改银行（使用已有聚合）
   *
   * @param model 银行
   * @param code 银行代码
   * @param name 银行名称
   * @param disabled 是否禁用
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   */
  public void modify(
      Bank model,
      String code,
      String name,
      boolean disabled,
      @Nullable Integer sequence,
      @Nullable String remark) {
    // 校验银行代码唯一
    if (bankRepository.isExistByCodeExcludeId(code, model.getId())) {
      throw new DomainException("银行代码" + code + "已存在!");
    }
    model.modify(code, name, disabled, sequence, remark);
  }
}
