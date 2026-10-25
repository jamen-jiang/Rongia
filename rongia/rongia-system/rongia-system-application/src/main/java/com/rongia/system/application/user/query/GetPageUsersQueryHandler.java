package com.rongia.system.application.user.query;

import com.rongia.shared.application.response.PageResponse;
import org.axonframework.messaging.queryhandling.annotation.QueryHandler;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;

/** 分页查询用户处理器 */
@NullMarked
@Component
public class GetPageUsersQueryHandler {

  /** 用户查询服务 */
  private final UserQueryService userQueryService;

  /**
   * 构造器注入
   *
   * @param userQueryService 用户查询服务
   */
  public GetPageUsersQueryHandler(UserQueryService userQueryService) {
    this.userQueryService = userQueryService;
  }

  /**
   * 处理查询
   *
   * @param query 查询
   * @return 分页响应
   */
  @QueryHandler
  public PageResponse<UserResponse> handle(GetPageUsersQuery query) {
    return userQueryService.getPage(query);
  }
}
