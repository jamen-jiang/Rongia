package com.rongia.basedata.domain.specattribute;

import com.rongia.shared.domain.exception.DomainException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 规格属性工厂，负责创建规格属性 */
@NullMarked
public class SpecAttributeFactory {

  /** 规格属性仓储 */
  private final SpecAttributeRepository specAttributeRepository;

  /**
   * 构造器注入
   *
   * @param specAttributeRepository 规格属性仓储
   */
  public SpecAttributeFactory(
      SpecAttributeRepository specAttributeRepository) {
    this.specAttributeRepository = specAttributeRepository;
  }

  /**
   * 创建规格属性
   *
   * @param categoryId 所属分类Id
   * @param code 规格属性代码
   * @param label 规格属性标签
   * @param inputType 输入类型
   * @param dataType 数据类型
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   * @return 规格属性聚合
   * @throws DomainException 同一分类下规格属性代码已存在时抛出
   */
  public SpecAttribute create(
      long categoryId,
      String code,
      String label,
      String inputType,
      String dataType,
      @Nullable Integer sequence,
      @Nullable String remark) {
    // 校验同一分类下规格属性代码唯一
    if (specAttributeRepository.isExistByCategoryIdAndCodeExcludeId(categoryId, code)) {
      throw new DomainException("分类" + categoryId + "下规格属性代码" + code + "已存在!");
    }
    return new SpecAttribute(
        categoryId, code, label, inputType, dataType, sequence, remark);
  }
}
