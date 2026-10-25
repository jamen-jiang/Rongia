package com.rongia.system.application.user.query;

import com.rongia.shared.application.response.PageResponse;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 用户查询服务 */
@NullMarked
public interface UserQueryService {

  /**
   * 按Id查询用户
   *
   * @param id 用户Id
   * @return 用户响应（可空），用户不存在返回 null
   */
  @Nullable UserResponse getById(long id);

  /**
   * 按账号查询用户详情（含角色列表 + 岗位列表）
   *
   * @param userName 账号
   * @return 用户响应（可空），不存在返回 null
   */
  @Nullable UserResponse getByUserName(String userName);

  /**
   * 分页查询用户
   *
   * @param query 分页查询
   * @return 分页响应
   */
  PageResponse<UserResponse> getPage(GetPageUsersQuery query);
}
