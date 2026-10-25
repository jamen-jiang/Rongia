package com.rongia.basedata.domain.specattribute;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 规格属性值数据
 *
 * @param value 规格属性值
 * @param label 规格属性值标签
 * @param isDefault 是否默认
 * @param sequence 排序（可空）
 */
@NullMarked
public record SpecAttributeValueData(
    String value, String label, Boolean isDefault, @Nullable Integer sequence) {}
