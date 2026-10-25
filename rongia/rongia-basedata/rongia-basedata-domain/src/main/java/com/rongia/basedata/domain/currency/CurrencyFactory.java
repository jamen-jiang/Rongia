package com.rongia.basedata.domain.currency;

import com.rongia.shared.domain.exception.DomainException;
import java.math.BigDecimal;
import org.jspecify.annotations.NullMarked;

/** 币种工厂，负责创建币种 */
@NullMarked
public class CurrencyFactory {

  /** 币种仓储 */
  private final CurrencyRepository currencyRepository;

  /**
   * 构造器注入
   *
   * @param currencyRepository 币种仓储
   */
  public CurrencyFactory(CurrencyRepository currencyRepository) {
    this.currencyRepository = currencyRepository;
  }

  /**
   * 创建币种
   *
   * @param code 币种代码
   * @param name 币种名称
   * @param symbol 币种符号
   * @param acceptRate 汇率
   * @param adjustRate 调整率
   * @param isStandard 是否本位币
   * @param disabled 是否禁用
   * @return 币种聚合
   * @throws DomainException 币种代码已存在时抛出
   */
  public Currency create(
      String code,
      String name,
      String symbol,
      BigDecimal acceptRate,
      BigDecimal adjustRate,
      boolean isStandard,
      boolean disabled) {
    // 校验币种代码唯一
    if (currencyRepository.isExistByCodeExcludeId(code)) {
      throw new DomainException("币种代码" + code + "已存在!");
    }
    return new Currency(code, name, symbol, acceptRate, adjustRate, isStandard, disabled);
  }
}
