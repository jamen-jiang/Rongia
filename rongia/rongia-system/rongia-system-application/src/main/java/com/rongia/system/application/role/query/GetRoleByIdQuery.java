package com.rongia.system.application.role.query;

import com.rongia.shared.application.cqrs.Query;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;

/** 按Id查询角色 */
@NullMarked
@Getter
@Setter
@Schema(description = "按Id查询角色")
public class GetRoleByIdQuery implements Query {

  /** 角色Id */
  @Schema(description = "角色Id", requiredMode = Schema.RequiredMode.REQUIRED)
  private Long id;

  /** 无参构造 */
  public GetRoleByIdQuery() {}

  /**
   * 有参构造
   *
   * @param id 角色Id
   */
  public GetRoleByIdQuery(long id) {
    this.id = id;
  }
}
