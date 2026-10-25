package com.rongia.system.application.department.query;

import com.rongia.shared.application.cqrs.Query;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;

/** 按Id获取部门 */
@NullMarked
@Getter
@Setter
@Schema(description = "按Id获取部门")
public class GetDepartmentByIdQuery implements Query {

  /** 部门Id */
  @Schema(description = "部门Id", requiredMode = Schema.RequiredMode.REQUIRED)
  private Long id;

  /** 无参构造 */
  public GetDepartmentByIdQuery() {}

  /**
   * 有参构造
   *
   * @param id 部门Id
   */
  public GetDepartmentByIdQuery(long id) {
    this.id = id;
  }
}
