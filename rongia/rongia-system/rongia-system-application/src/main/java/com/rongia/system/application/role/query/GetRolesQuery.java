package com.rongia.system.application.role.query;

import com.rongia.shared.application.cqrs.Query;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;

/** 查询所有角色列表 */
@NullMarked
@Getter
@Setter
@Schema(description = "查询所有角色列表")
public class GetRolesQuery implements Query {}
