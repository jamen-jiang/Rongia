package com.rongia.system.application.position.query;

import com.rongia.shared.application.exception.ApiException;
import org.axonframework.messaging.queryhandling.annotation.QueryHandler;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/** 按Id查询岗位查询处理器 */
@NullMarked
@Component
public class GetPositionByIdQueryHandler {

  /** 岗位查询服务 */
  private final PositionQueryService postQueryService;

  /**
   * 构造器注入
   *
   * @param postQueryService 岗位查询服务
   */
  public GetPositionByIdQueryHandler(PositionQueryService postQueryService) {
    this.postQueryService = postQueryService;
  }

  /**
   * 处理查询
   *
   * @param query 查询
   * @return 岗位响应
   */
  @QueryHandler
  public @Nullable PositionResponse handle(GetPositionByIdQuery query) {
    PositionResponse response = postQueryService.getById(query.getId());
    if (response == null) {
      throw new ApiException("岗位不存在!");
    }
    return response;
  }
}
