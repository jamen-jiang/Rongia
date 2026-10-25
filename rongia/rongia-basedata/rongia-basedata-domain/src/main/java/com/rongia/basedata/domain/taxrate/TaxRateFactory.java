package com.rongia.basedata.domain.taxrate;

import com.rongia.shared.domain.exception.DomainException;
import java.math.BigDecimal;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 税率工厂，负责创建税率 */
@NullMarked
public class TaxRateFactory {

  /** 税率仓储 */
  private final TaxRateRepository taxRateRepository;

  /**
   * 构造器注入
   *
   * @param taxRateRepository 税率仓储
   */
  public TaxRateFactory(TaxRateRepository taxRateRepository) {
    this.taxRateRepository = taxRateRepository;
  }

  /**
   * 创建税率
   *
   * @param code 代码
   * @param name 名称
   * @param rate 税率值
   * @param disabled 是否禁用
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   * @return 税率聚合
   * @throws DomainException 税率代码已存在时抛出
   */
  public TaxRate create(
      String code,
      String name,
      BigDecimal rate,
      boolean disabled,
      @Nullable Integer sequence,
      @Nullable String remark) {
    // 校验税率代码唯一
    if (taxRateRepository.isExistByCodeExcludeId(code)) {
      throw new DomainException("税率代码" + code + "已存在!");
    }
    return new TaxRate(code, name, rate, disabled, sequence, remark);
  }
}
