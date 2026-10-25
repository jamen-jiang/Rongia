package com.rongia.system.application.department.query;

import org.axonframework.messaging.queryhandling.annotation.QueryHandler;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/** 按Id获取部门查询处理器 */
@NullMarked
@Component
public class GetDepartmentByIdQueryHandler {

  /** 部门查询服务 */
  private final DepartmentQueryService departmentQueryService;

  /**
   * 构造器注入
   *
   * @param departmentQueryService 部门查询服务
   */
  public GetDepartmentByIdQueryHandler(DepartmentQueryService departmentQueryService) {
    this.departmentQueryService = departmentQueryService;
  }

  /**
   * 处理查询
   *
   * @param query 查询
   * @return 部门响应
   */
  @QueryHandler
  public @Nullable DepartmentResponse handle(GetDepartmentByIdQuery query) {
    return departmentQueryService.getById(query.getId());
  }
}
