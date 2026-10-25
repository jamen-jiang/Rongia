package com.rongia.system.application.signinlog.query;

import com.rongia.shared.application.response.PageResponse;
import org.axonframework.messaging.queryhandling.annotation.QueryHandler;
import org.springframework.stereotype.Component;

/** 分页查询登录日志列表查询处理器 */
@Component
public class GetPageSignInLogsQueryHandler {

  /** 登录日志查询服务 */
  private final SignInLogQueryService signInLogQueryService;

  /**
   * 构造器注入
   *
   * @param signInLogQueryService 登录日志查询服务
   */
  public GetPageSignInLogsQueryHandler(SignInLogQueryService signInLogQueryService) {
    this.signInLogQueryService = signInLogQueryService;
  }

  /**
   * 处理查询
   *
   * @param query 查询
   * @return 分页响应
   */
  @QueryHandler
  public PageResponse<SignInLogResponse> handle(GetPageSignInLogsQuery query) {
    return signInLogQueryService.getPage(query);
  }
}
