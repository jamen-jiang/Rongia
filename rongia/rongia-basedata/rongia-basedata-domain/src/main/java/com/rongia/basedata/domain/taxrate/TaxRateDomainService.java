package com.rongia.basedata.domain.taxrate;

import com.rongia.shared.domain.exception.DomainException;
import java.math.BigDecimal;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 税率领域服务，负责跨聚合编排 */
@NullMarked
public class TaxRateDomainService {

  /** 税率工厂 */
  private final TaxRateFactory taxRateFactory;

  /** 税率仓储 */
  private final TaxRateRepository taxRateRepository;

  /**
   * 构造器注入
   *
   * @param taxRateFactory 税率工厂
   * @param taxRateRepository 税率仓储
   */
  public TaxRateDomainService(
      TaxRateFactory taxRateFactory, TaxRateRepository taxRateRepository) {
    this.taxRateFactory = taxRateFactory;
    this.taxRateRepository = taxRateRepository;
  }

  /**
   * 导入生成：税率存在则修改，不存在则创建
   *
   * @param code 税率代码
   * @param name 税率名称
   * @param rate 税率值
   * @param disabled 是否禁用
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   * @return 税率聚合
   */
  public TaxRate importGenerate(
      String code,
      String name,
      BigDecimal rate,
      boolean disabled,
      @Nullable Integer sequence,
      @Nullable String remark) {
    // 按代码查税率
    TaxRate model = taxRateRepository.getByCode(code);

    // 不存在则创建，存在则修改
    if (model == null) {
      model = taxRateFactory.create(code, name, rate, disabled, sequence, remark);
    } else {
      modify(model, code, name, rate, disabled, sequence, remark);
    }

    return model;
  }

  /**
   * 修改税率
   *
   * @param id 税率Id
   * @param code 税率代码
   * @param name 税率名称
   * @param rate 税率值
   * @param disabled 是否禁用
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   * @return 税率聚合
   */
  public TaxRate modify(
      long id,
      String code,
      String name,
      BigDecimal rate,
      boolean disabled,
      @Nullable Integer sequence,
      @Nullable String remark) {
    TaxRate model = taxRateRepository.get(id);
    if (model == null) {
      throw new DomainException("税率" + id + "不存在!");
    }
    modify(model, code, name, rate, disabled, sequence, remark);
    return model;
  }

  /**
   * 修改税率
   *
   * @param model 税率
   * @param code 税率代码
   * @param name 税率名称
   * @param rate 税率值
   * @param disabled 是否禁用
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   */
  public void modify(
      TaxRate model,
      String code,
      String name,
      BigDecimal rate,
      boolean disabled,
      @Nullable Integer sequence,
      @Nullable String remark) {
    // 校验税率代码唯一
    if (taxRateRepository.isExistByCodeExcludeId(code, model.getId())) {
      throw new DomainException("税率代码" + code + "已存在!");
    }
    model.modify(code, name, rate, disabled, sequence, remark);
  }
}
