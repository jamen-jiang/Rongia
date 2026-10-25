package com.rongia.system.application.menu.query;

import org.axonframework.messaging.queryhandling.annotation.QueryHandler;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/** 按Id查询菜单处理器 */
@NullMarked
@Component
public class GetMenuByIdQueryHandler {

  /** 查询服务 */
  private final MenuQueryService menuQueryService;

  /**
   * 构造器注入
   *
   * @param menuQueryService 查询服务
   */
  public GetMenuByIdQueryHandler(MenuQueryService menuQueryService) {
    this.menuQueryService = menuQueryService;
  }

  /**
   * 处理查询
   *
   * @param query 查询
   * @return 菜单响应
   */
  @QueryHandler
  public @Nullable MenuResponse handle(GetMenuByIdQuery query) {
    return menuQueryService.getById(query.getId());
  }
}
