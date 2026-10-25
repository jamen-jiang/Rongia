package com.rongia.basedata.domain.specattribute;

import org.jspecify.annotations.NullMarked;

/**
 * 挂到规格上的规格属性值数据（由领域服务解析出定义后交给聚合成子表行）
 *
 * @param specAttributeId 规格属性定义Id
 * @param code 规格属性代码（冗余存一份，读值时不必再回定义表）
 * @param value 规格属性值
 */
@NullMarked
public record AssignedSpecAttributeData(
    Long specAttributeId, String code, String value) {}
