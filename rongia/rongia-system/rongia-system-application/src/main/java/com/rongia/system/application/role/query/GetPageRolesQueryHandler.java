package com.rongia.system.application.role.query;

import com.rongia.shared.application.response.PageResponse;
import org.axonframework.messaging.queryhandling.annotation.QueryHandler;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;

/** 分页查询角色列表查询处理器 */
@NullMarked
@Component
public class GetPageRolesQueryHandler {

  /** 角色查询服务 */
  private final RoleQueryService roleQueryService;

  /**
   * 构造器注入
   *
   * @param roleQueryService 角色查询服务
   */
  public GetPageRolesQueryHandler(RoleQueryService roleQueryService) {
    this.roleQueryService = roleQueryService;
  }

  /**
   * 处理查询
   *
   * @param query 查询
   * @return 分页响应
   */
  @QueryHandler
  public PageResponse<RoleResponse> handle(GetPageRolesQuery query) {
    return roleQueryService.getPage(query);
  }
}
