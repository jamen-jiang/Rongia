package com.rongia.system.application.position.query;

import com.rongia.shared.application.cqrs.Query;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;

/** 查询所有岗位列表 */
@NullMarked
@Getter
@Setter
@Schema(description = "查询所有岗位列表")
public class GetPositionsQuery implements Query {}
