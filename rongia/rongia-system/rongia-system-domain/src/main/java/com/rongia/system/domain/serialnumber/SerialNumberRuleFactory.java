package com.rongia.system.domain.serialnumber;

import com.rongia.shared.domain.exception.DomainException;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 流水号规则工厂，负责创建规则 */
@NullMarked
public class SerialNumberRuleFactory {

  /** 流水号规则仓储 */
  private final SerialNumberRuleRepository ruleRepository;

  /**
   * 构造器注入
   *
   * @param ruleRepository 流水号规则仓储
   */
  public SerialNumberRuleFactory(SerialNumberRuleRepository ruleRepository) {
    this.ruleRepository = ruleRepository;
  }

  /**
   * 创建规则
   *
   * @param type 类型（对应流水号类型编码，如 SalesOrder）
   * @param rule 计数重置周期（可空=永不重置，Year/Month/Day）
   * @param prefix 前缀模板（空串=无前缀）
   * @param suffix 后缀模板（空串=无后缀）
   * @param pad 补零位数
   * @param step 步长
   * @param remark 备注（可空）
   * @return 流水号规则聚合
   * @throws DomainException 类型已存在或字段校验不过时抛出
   */
  public SerialNumberRule create(
      String type,
      @Nullable String rule,
      String prefix,
      String suffix,
      int pad,
      int step,
      @Nullable String remark) {
    String trimmedType = StringUtils.trimToEmpty(type);
    if (ruleRepository.isExistByTypeExcludeId(trimmedType)) {
      throw new DomainException("流水号规则类型" + trimmedType + "已存在!");
    }
    return new SerialNumberRule(trimmedType, rule, prefix, suffix, pad, step, remark);
  }
}
