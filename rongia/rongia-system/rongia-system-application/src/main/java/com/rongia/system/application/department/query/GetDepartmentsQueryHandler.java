package com.rongia.system.application.department.query;

import java.util.List;
import org.axonframework.messaging.queryhandling.annotation.QueryHandler;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;

/** 获取所有部门列表查询处理器 */
@NullMarked
@Component
public class GetDepartmentsQueryHandler {

  /** 部门查询服务 */
  private final DepartmentQueryService departmentQueryService;

  /**
   * 构造器注入
   *
   * @param departmentQueryService 部门查询服务
   */
  public GetDepartmentsQueryHandler(DepartmentQueryService departmentQueryService) {
    this.departmentQueryService = departmentQueryService;
  }

  /**
   * 处理查询
   *
   * @param query 查询
   * @return 部门响应列表
   */
  @QueryHandler
  public List<DepartmentResponse> handle(GetDepartmentsQuery query) {
    return departmentQueryService.getList(query);
  }
}
