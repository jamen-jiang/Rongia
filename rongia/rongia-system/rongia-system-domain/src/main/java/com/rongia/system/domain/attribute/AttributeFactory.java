package com.rongia.system.domain.attribute;

import com.rongia.shared.domain.exception.DomainException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 属性工厂，负责创建属性 */
@NullMarked
public class AttributeFactory {

  /** 属性仓储 */
  private final AttributeRepository attributeRepository;

  /**
   * 构造器注入
   *
   * @param attributeRepository 属性仓储
   */
  public AttributeFactory(AttributeRepository attributeRepository) {
    this.attributeRepository = attributeRepository;
  }

  /**
   * 创建属性
   *
   * @param entityType 持有者表名（由持有者自己传，如 {@code Material} / {@code Variant}）
   * @param code 属性代码
   * @param label 属性标签
   * @param inputType 输入类型
   * @param dataType 数据类型
   * @param isRequired 是否必填
   * @param isFilter 是否作为筛选项
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   * @return 属性聚合
   * @throws DomainException 同一持有者类型下属性代码已存在时抛出
   */
  public Attribute create(
      String entityType,
      String code,
      String label,
      String inputType,
      String dataType,
      boolean isRequired,
      boolean isFilter,
      @Nullable Integer sequence,
      @Nullable String remark) {
    // 校验同一持有者类型下属性代码唯一
    if (attributeRepository.isExistByEntityTypeAndCodeExcludeId(entityType, code)) {
      throw new DomainException(entityType + "下属性代码" + code + "已存在!");
    }
    return new Attribute(
        entityType, code, label, inputType, dataType, isRequired, isFilter, sequence, remark);
  }
}
