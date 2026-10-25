package com.rongia.shared.adapter.advice;

import com.rongia.shared.application.response.ApiDataResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import tools.jackson.databind.ObjectMapper;

/** 统一响应包装 */
@RestControllerAdvice(basePackages = "com.rongia")
public class ResponseWrapperAdvice implements ResponseBodyAdvice<Object> {

  /** JSON 序列化器（Spring 默认，用于 Web 序列化） */
  private final ObjectMapper objectMapper;

  /**
   * 构造器注入
   *
   * @param objectMapper JSON 序列化器
   */
  public ResponseWrapperAdvice(@Qualifier("jacksonJsonMapper") ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * 判断是否需要包装
   *
   * @param returnType 返回类型
   * @param converterType 消息转换器类型
   * @return true 表示需要包装
   */
  @Override
  public boolean supports(
      @NonNull MethodParameter returnType,
      @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
    // 已经是 ApiResponse 的不再包和ResponseEntity 不包（文件下载、自定义状态码等场景）
    if (ApiDataResponse.class.isAssignableFrom(returnType.getParameterType())
        || ResponseEntity.class.isAssignableFrom(returnType.getParameterType())) {
      return false;
    }
    return true;
  }

  /**
   * 包装响应体
   *
   * @param body 原响应体
   * @param returnType 返回类型
   * @param selectedContentType 内容类型
   * @param selectedConverterType 转换器类型
   * @param request 请求
   * @param response 响应
   * @return 包装后的响应
   */
  @Override
  public Object beforeBodyWrite(
      Object body,
      @NonNull MethodParameter returnType,
      @NonNull MediaType selectedContentType,
      @NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
      @NonNull ServerHttpRequest request,
      @NonNull ServerHttpResponse response) {

    // 已经是 ApiResponse 直接返回
    if (body instanceof ApiDataResponse) {
      return body;
    }

    // String 类型需手动转 JSON，否则 StringHttpMessageConverter 会 ClassCastException
    if (body instanceof String) {
      response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
      return objectMapper.writeValueAsString(ApiDataResponse.success(body));
    }

    return ApiDataResponse.success(body);
  }
}
