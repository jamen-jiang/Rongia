package com.rongia.system.domain.attribute;

import org.jspecify.annotations.NullMarked;

/**
 * 挂到对象上的属性值数据（由领域服务解析出定义后交给聚合落成子表行）
 *
 * @param attributeId 属性定义Id
 * @param code 属性代码（冗余存一份，读值时不必再回定义表）
 * @param value 属性值
 */
@NullMarked
public record AssignedAttributeData(Long attributeId, String code, String value) {}
