package com.rongia.platform.web;

import cn.dev33.satoken.exception.NotLoginException;
import com.rongia.kernel.application.enums.ApiStatusEnum;
import com.rongia.kernel.application.exception.ApiException;
import com.rongia.kernel.application.response.ApiResponse;
import com.rongia.kernel.domain.exception.DomainException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 全局异常处理器 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * 处理未登录异常（Sa-Token 路由拦截器抛出）
   *
   * @param e 未登录异常
   * @return 统一响应
   */
  @ExceptionHandler(NotLoginException.class)
  @ResponseStatus(HttpStatus.OK)
  public ApiResponse<Void> handleNotLogin(NotLoginException e) {
    // 被踢下线 / 被顶下线 / 已过期各有专属文案，其余按未登录处理
    String type = e.getType();
    if (NotLoginException.KICK_OUT.equals(type) || NotLoginException.BE_REPLACED.equals(type)) {
      return ApiResponse.error(ApiStatusEnum.UNAUTHORIZED, "登录已在其他设备失效，请重新登录");
    }
    if (NotLoginException.TOKEN_TIMEOUT.equals(type) || NotLoginException.TOKEN_FREEZE.equals(type)) {
      return ApiResponse.error(ApiStatusEnum.UNAUTHORIZED, "登录已过期，请重新登录");
    }
    return ApiResponse.error(ApiStatusEnum.UNAUTHORIZED, "未登录");
  }

  /**
   * 处理全局异常
   *
   * @param e 异常
   * @return 统一响应
   */
  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.OK)
  public ApiResponse<Void> handleException(Exception e) {
    // 核心修改点：先提取根本原因（穿透 CompletionException 等异步包装异常）
    Throwable rootCause = NestedExceptionUtils.getMostSpecificCause(e);

    // 业务异常：直接用异常自带的状态码和信息（使用 rootCause 进行判断）
    if (rootCause instanceof ApiException apiException) {
      return ApiResponse.error(apiException.getCode(), apiException.getMessage());
    }
    // 领域异常：固定 FAIL，不打 error 日志
    if (rootCause instanceof DomainException domainException) {
      return ApiResponse.error(ApiStatusEnum.FAIL, domainException.getMessage());
    }

    // 未预期异常：打 error 日志（会写进 SystemLog），再返回系统异常
    log.error("系统异常：{}", rootCause.getMessage(), rootCause);
    return ApiResponse.error(ApiStatusEnum.ERROR, "系统异常");
  }
}
