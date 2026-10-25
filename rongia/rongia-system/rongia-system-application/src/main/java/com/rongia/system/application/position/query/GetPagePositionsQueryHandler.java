package com.rongia.system.application.position.query;

import com.rongia.shared.application.response.PageResponse;
import org.axonframework.messaging.queryhandling.annotation.QueryHandler;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;

/** 分页查询岗位列表查询处理器 */
@NullMarked
@Component
public class GetPagePositionsQueryHandler {

  /** 岗位查询服务 */
  private final PositionQueryService postQueryService;

  /**
   * 构造器注入
   *
   * @param postQueryService 岗位查询服务
   */
  public GetPagePositionsQueryHandler(PositionQueryService postQueryService) {
    this.postQueryService = postQueryService;
  }

  /**
   * 处理查询
   *
   * @param query 查询
   * @return 分页响应
   */
  @QueryHandler
  public PageResponse<PositionResponse> handle(GetPagePositionsQuery query) {
    return postQueryService.getPage(query);
  }
}
