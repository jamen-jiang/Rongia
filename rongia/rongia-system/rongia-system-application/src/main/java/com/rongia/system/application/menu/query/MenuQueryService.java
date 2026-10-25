package com.rongia.system.application.menu.query;

import java.util.Collection;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 菜单查询服务接口 */
@NullMarked
public interface MenuQueryService {

  /**
   * 按Id查询
   *
   * @param id 菜单Id
   * @return 菜单响应
   */
  @Nullable MenuResponse getById(long id);

  /**
   * 按Id列表查询
   *
   * @param ids 菜单Id列表（可空）
   * @return 菜单响应列表
   */
  List<MenuResponse> getListByIds(Collection<Long> ids);

  /**
   * 查询所有
   *
   * @return 菜单响应列表
   */
  List<MenuResponse> getList();
}
