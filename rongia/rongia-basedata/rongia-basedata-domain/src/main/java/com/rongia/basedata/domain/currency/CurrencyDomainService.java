package com.rongia.basedata.domain.currency;

import com.rongia.shared.domain.exception.DomainException;
import java.math.BigDecimal;
import org.jspecify.annotations.NullMarked;

/** 币种领域服务，负责跨聚合编排 */
@NullMarked
public class CurrencyDomainService {

  /** 币种工厂 */
  private final CurrencyFactory currencyFactory;

  /** 币种仓储 */
  private final CurrencyRepository currencyRepository;

  /**
   * 构造器注入
   *
   * @param currencyFactory 币种工厂
   * @param currencyRepository 币种仓储
   */
  public CurrencyDomainService(
      CurrencyFactory currencyFactory, CurrencyRepository currencyRepository) {
    this.currencyFactory = currencyFactory;
    this.currencyRepository = currencyRepository;
  }

  /**
   * 导入生成：币种存在则修改，不存在则创建
   *
   * @param code 币种代码
   * @param name 币种名称
   * @param symbol 币种符号
   * @param acceptRate 汇率
   * @param adjustRate 调整率
   * @param isStandard 是否本位币
   * @param disabled 是否禁用
   * @return 币种聚合
   */
  public Currency importGenerate(
      String code,
      String name,
      String symbol,
      BigDecimal acceptRate,
      BigDecimal adjustRate,
      boolean isStandard,
      boolean disabled) {
    // 按代码查币种
    Currency model = currencyRepository.getByCode(code);

    // 不存在则创建，存在则修改
    if (model == null) {
      model =
          currencyFactory.create(code, name, symbol, acceptRate, adjustRate, isStandard, disabled);
    } else {
      modify(model, code, name, symbol, acceptRate, adjustRate, isStandard, disabled);
    }

    return model;
  }

  /**
   * 创建币种
   *
   * @param id 币种Id
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
  public Currency modify(
      long id,
      String code,
      String name,
      String symbol,
      BigDecimal acceptRate,
      BigDecimal adjustRate,
      boolean isStandard,
      boolean disabled) {
    Currency model = currencyRepository.get(id);
    if (model == null) {
      throw new DomainException("币种" + id + "不存在!");
    }
    modify(model, code, name, symbol, acceptRate, adjustRate, isStandard, disabled);
    return model;
  }

  /**
   * 创建币种
   *
   * @param model 币种
   * @param code 币种代码
   * @param name 币种名称
   * @param symbol 币种符号
   * @param acceptRate 汇率
   * @param adjustRate 调整率
   * @param isStandard 是否本位币
   * @param disabled 是否禁用
   * @throws DomainException 币种代码已存在时抛出
   */
  public void modify(
      Currency model,
      String code,
      String name,
      String symbol,
      BigDecimal acceptRate,
      BigDecimal adjustRate,
      boolean isStandard,
      boolean disabled) {
    // 校验币种代码唯一
    if (currencyRepository.isExistByCodeExcludeId(code, model.getId())) {
      throw new DomainException("币种代码" + code + "已存在!");
    }
    model.modify(code, name, symbol, acceptRate, adjustRate, isStandard, disabled);
  }
}
