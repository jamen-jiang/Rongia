package com.rongia.system.application.role.query;

import com.rongia.shared.application.exception.ApiException;
import org.axonframework.messaging.queryhandling.annotation.QueryHandler;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/** 按Id查询角色查询处理器 */
@NullMarked
@Component
public class GetRoleByIdQueryHandler {

  /** 角色查询服务 */
  private final RoleQueryService roleQueryService;

  /**
   * 构造器注入
   *
   * @param roleQueryService 角色查询服务
   */
  public GetRoleByIdQueryHandler(RoleQueryService roleQueryService) {
    this.roleQueryService = roleQueryService;
  }

  /**
   * 处理查询
   *
   * @param query 查询
   * @return 角色响应
   */
  @QueryHandler
  public @Nullable RoleResponse handle(GetRoleByIdQuery query) {
    RoleResponse response = roleQueryService.getById(query.getId());
    if (response == null) {
      throw new ApiException("角色不存在!");
    }
    return response;
  }
}
