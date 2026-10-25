package com.rongia.system.domain.attribute;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 属性值数据
 *
 * @param value 属性值
 * @param label 属性值标签
 * @param isDefault 是否默认
 * @param sequence 排序（可空）
 */
@NullMarked
public record AttributeValueData(
    String value, String label, Boolean isDefault, @Nullable Integer sequence) {}
