package com.rongia.system.application.role.query;

import com.rongia.shared.application.response.PageResponse;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 角色查询服务接口 */
@NullMarked
public interface RoleQueryService {

  /**
   * 按Id查询
   *
   * @param id 角色Id
   * @return 角色响应（可空），不存在返回 null
   */
  @Nullable RoleResponse getById(long id);

  /**
   * 查询所有角色
   *
   * @return 角色响应列表
   */
  List<RoleResponse> getList();

  /**
   * 分页查询角色
   *
   * @param query 分页查询
   * @return 分页响应
   */
  PageResponse<RoleResponse> getPage(GetPageRolesQuery query);
}
