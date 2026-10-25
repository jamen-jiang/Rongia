package com.rongia.system.application.department.query;

import com.rongia.shared.application.cqrs.Query;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 获取所有部门列表 */
@NullMarked
@Getter
@Setter
@Schema(description = "获取所有部门列表")
public class GetDepartmentsQuery implements Query {

  /** 公司Id（可空） */
  @Schema(description = "公司Id")
  @Nullable
  private Long companyId;

  /** 父Id（可空） */
  @Schema(description = "父Id")
  @Nullable
  private Long parentId;

  /** 无参构造 */
  public GetDepartmentsQuery() {}

  /**
   * 有参构造
   *
   * @param companyId 公司Id
   * @param parentId 父Id
   */
  public GetDepartmentsQuery(@Nullable Long companyId, @Nullable Long parentId) {
    this.companyId = companyId;
    this.parentId = parentId;
  }
}
