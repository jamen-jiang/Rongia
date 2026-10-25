package com.rongia.system.application.menu.query;

import java.util.List;
import org.axonframework.messaging.queryhandling.annotation.QueryHandler;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;

/** 查询所有菜单处理器 */
@NullMarked
@Component
public class GetMenusQueryHandler {

  /** 查询服务 */
  private final MenuQueryService menuQueryService;

  /**
   * 构造器注入
   *
   * @param menuQueryService 查询服务
   */
  public GetMenusQueryHandler(MenuQueryService menuQueryService) {
    this.menuQueryService = menuQueryService;
  }

  /**
   * 处理查询
   *
   * @param query 查询
   * @return 菜单响应列表
   */
  @QueryHandler
  public List<MenuResponse> handle(GetMenusQuery query) {
    return menuQueryService.getList();
  }
}
