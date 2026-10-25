package com.rongia.system.application.role.query;

import java.util.List;
import org.axonframework.messaging.queryhandling.annotation.QueryHandler;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;

/** 查询所有角色列表查询处理器 */
@NullMarked
@Component
public class GetRolesQueryHandler {

  /** 角色查询服务 */
  private final RoleQueryService roleQueryService;

  /**
   * 构造器注入
   *
   * @param roleQueryService 角色查询服务
   */
  public GetRolesQueryHandler(RoleQueryService roleQueryService) {
    this.roleQueryService = roleQueryService;
  }

  /**
   * 处理查询
   *
   * @param query 查询
   * @return 角色响应列表
   */
  @QueryHandler
  public List<RoleResponse> handle(GetRolesQuery query) {
    return roleQueryService.getList();
  }
}
