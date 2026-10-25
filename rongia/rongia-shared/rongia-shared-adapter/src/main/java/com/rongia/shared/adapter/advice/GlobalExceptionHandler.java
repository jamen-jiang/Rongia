package com.rongia.shared.adapter.advice;

import com.rongia.shared.application.enums.ApiStatusEnum;
import com.rongia.shared.application.exception.ApiException;
import com.rongia.shared.application.response.ApiDataResponse;
import com.rongia.shared.domain.exception.DomainException;
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
   * 处理全局异常
   *
   * @param e 异常
   * @return 统一响应
   */
  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.OK)
  public ApiDataResponse<Void> handleException(Exception e) {
    // 核心修改点：先提取根本原因（穿透 CompletionException 等异步包装异常）
    Throwable rootCause = NestedExceptionUtils.getMostSpecificCause(e);

    // 业务异常：直接用异常自带的状态码和信息（使用 rootCause 进行判断）
    if (rootCause instanceof ApiException apiException) {
      return ApiDataResponse.error(apiException.getCode(), apiException.getMessage());
    }
    // 领域异常：固定 Fail_App，不打 error 日志
    if (rootCause instanceof DomainException domainException) {
      return ApiDataResponse.error(ApiStatusEnum.FAIL_APP, domainException.getMessage());
    }

    // 未预期异常：打 error 日志（会写进 SystemLog），再返回系统异常
    log.error("系统异常：{}", rootCause.getMessage(), rootCause);
    return ApiDataResponse.error(ApiStatusEnum.FAIL_EXCEPTION, "系统异常");
  }
}
