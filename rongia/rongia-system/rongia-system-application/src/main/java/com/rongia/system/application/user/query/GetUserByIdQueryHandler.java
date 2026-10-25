package com.rongia.system.application.user.query;

import org.axonframework.messaging.queryhandling.annotation.QueryHandler;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/** 按Id查询用户处理器 */
@NullMarked
@Component
public class GetUserByIdQueryHandler {

  /** 用户查询服务 */
  private final UserQueryService userQueryService;

  /**
   * 构造器注入
   *
   * @param userQueryService 用户查询服务
   */
  public GetUserByIdQueryHandler(UserQueryService userQueryService) {
    this.userQueryService = userQueryService;
  }

  /**
   * 处理查询
   *
   * @param query 查询
   * @return 用户响应（可空），用户不存在返回 null
   */
  @QueryHandler
  public @Nullable UserResponse handle(GetUserByIdQuery query) {
    return userQueryService.getById(query.getId());
  }
}
