package com.rongia.system.domain.attribute;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 属性领域服务，负责属性子实体同步 */
@NullMarked
public class AttributeDomainService {

  /**
   * 同步属性值列表（按 Value 业务标识匹配：删除不在输入中的、更新已存在的、添加新的）
   *
   * @param attribute 属性聚合
   * @param attributeValues 目标属性值列表（可空）
   */
  public void syncAttributeValues(
      Attribute attribute, @Nullable List<AttributeValueData> attributeValues) {
    List<AttributeValueData> target = attributeValues == null ? List.of() : attributeValues;

    // 1. 删除未在输入中出现的属性值（按 Value 匹配）
    List<AttributeValue> toRemoves =
        attribute.getAttributeValues().stream()
            .filter(i -> target.stream().noneMatch(d -> Objects.equals(d.value(), i.getValue())))
            .toList();
    for (AttributeValue attributeValue : toRemoves) {
      attribute.deleteValue(attributeValue.getId());
    }

    // 2. 建立"Value → 已存在属性值Id"的映射（只统计未删除的）
    Map<String, Long> existingDict =
        attribute.getAttributeValues().stream()
            .filter(i -> !i.getIsDeleted())
            .collect(
                Collectors.toMap(
                    AttributeValue::getValue, AttributeValue::getId, (a, b) -> a));

    // 3. 遍历输入：存在则修改，不存在则添加
    for (AttributeValueData data : target) {
      Long id = existingDict.get(data.value());
      if (id != null) {
        // 存在：修改
        attribute.modifyValue(id, data.label(), data.isDefault(), data.sequence());
      } else {
        // 不存在：添加
        attribute.addValue(data.value(), data.label(), data.isDefault(), data.sequence());
      }
    }
  }
}
