package com.rongia.system.application.signinlog.query;

import com.rongia.shared.application.response.PageResponse;
import org.jspecify.annotations.NullMarked;

/** 登录日志查询服务接口 */
@NullMarked
public interface SignInLogQueryService {

  /**
   * 分页查询登录日志（全字段，含错误信息 / 异常信息）
   *
   * @param query 分页查询
   * @return 分页响应
   */
  PageResponse<SignInLogResponse> getPage(GetPageSignInLogsQuery query);
}
