package com.rongia.basedata.domain.specattribute;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 规格属性领域服务，负责规格属性子实体同步 */
@NullMarked
public class SpecAttributeDomainService {

  /**
   * 同步规格属性值列表（按 Value 业务标识匹配：删除不在输入中的、更新已存在的、添加新的）
   *
   * @param specAttribute 规格属性聚合
   * @param specAttributeValues 目标规格属性值列表（可空）
   */
  public void syncSpecAttributeValues(
      SpecAttribute specAttribute,
      @Nullable List<SpecAttributeValueData> specAttributeValues) {
    List<SpecAttributeValueData> target =
        specAttributeValues == null ? List.of() : specAttributeValues;

    // 1. 删除未在输入中出现的规格属性值（按 Value 匹配）
    List<SpecAttributeValue> toRemoves =
        specAttribute.getSpecAttributeValues().stream()
            .filter(i -> target.stream().noneMatch(d -> Objects.equals(d.value(), i.getValue())))
            .toList();
    for (SpecAttributeValue specAttributeValue : toRemoves) {
      specAttribute.deleteValue(specAttributeValue.getId());
    }

    // 2. 建立"Value → 已存在规格属性值Id"的映射（只统计未删除的）
    Map<String, Long> existingDict =
        specAttribute.getSpecAttributeValues().stream()
            .filter(i -> !i.getIsDeleted())
            .collect(
                Collectors.toMap(
                    SpecAttributeValue::getValue,
                    SpecAttributeValue::getId,
                    (a, b) -> a));

    // 3. 遍历输入：存在则修改，不存在则添加
    for (SpecAttributeValueData data : target) {
      Long id = existingDict.get(data.value());
      if (id != null) {
        // 存在：修改
        specAttribute.modifyValue(id, data.label(), data.isDefault(), data.sequence());
      } else {
        // 不存在：添加
        specAttribute.addValue(data.value(), data.label(), data.isDefault(), data.sequence());
      }
    }
  }
}
