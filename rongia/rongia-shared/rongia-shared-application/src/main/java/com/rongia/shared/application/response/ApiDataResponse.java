package com.rongia.shared.application.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rongia.shared.application.enums.ApiStatusEnum;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 统一响应包装
 *
 * @param <T> 数据类型
 */
@NullMarked
@Getter
public class ApiDataResponse<T> {

  /** 状态码 */
  private final Integer status;

  /** 信息（可空） */
  private @Nullable String message;

  /** 结果集（可空） */
  private @Nullable T data;

  /** 默认构造器；状态码取 SUCCESS */
  public ApiDataResponse() {
    this.status = ApiStatusEnum.SUCCESS.getCode();
  }

  /**
   * 构造器
   *
   * @param data 结果集（可空）
   */
  public ApiDataResponse(T data) {
    this.data = data;
    this.status = ApiStatusEnum.SUCCESS.getCode();
  }

  /**
   * 构造器
   *
   * @param message 信息（可空）
   */
  public ApiDataResponse(String message) {
    this.message = message;
    this.status = ApiStatusEnum.SUCCESS.getCode();
  }

  /**
   * 构造器
   *
   * @param apiStatusEnum 状态枚举
   * @param message 信息（可空）
   */
  public ApiDataResponse(ApiStatusEnum apiStatusEnum, String message) {
    this.status = apiStatusEnum.getCode();
    this.message = message;
  }

  /**
   * 构造器
   *
   * @param status 状态码
   * @param message 信息（可空）
   */
  public ApiDataResponse(int status, String message) {
    this.status = status;
    this.message = message;
  }

  /**
   * 获取信息；自定义为空时回退到状态枚举的描述
   *
   * @return 信息（可空）
   */
  public String getMessage() {
    if (message == null || message.isBlank()) {
      ApiStatusEnum statusEnum = ApiStatusEnum.fromCode(status);
      return statusEnum == null ? "" : statusEnum.getLabel();
    }
    return message;
  }

  /**
   * 获取信息（Msg 别名）
   *
   * @return 信息（可空）
   */
  public String getMsg() {
    return getMessage();
  }

  /**
   * 是否成功
   *
   * @return true 表示成功
   */
  @JsonProperty("isSuccess")
  public boolean isSuccess() {
    return status == ApiStatusEnum.SUCCESS.getCode();
  }

  // ---------- 静态工厂方法 ----------

  /**
   * 成功响应（无数据）
   *
   * @param <T> 数据类型
   * @return 响应
   */
  public static <T> ApiDataResponse<T> success() {
    return new ApiDataResponse<>();
  }

  /**
   * 成功响应（带数据）
   *
   * @param data 数据（可空）
   * @param <T> 数据类型
   * @return 响应
   */
  public static <T> ApiDataResponse<T> success(T data) {
    return new ApiDataResponse<>(data);
  }

  /**
   * 成功响应（带信息）
   *
   * @param message 信息（可空）
   * @param <T> 数据类型
   * @return 响应
   */
  public static <T> ApiDataResponse<T> successMessage(String message) {
    return new ApiDataResponse<>(message);
  }

  /**
   * 失败响应（默认 FAIL_APP）
   *
   * @param message 信息（可空）
   * @param <T> 数据类型
   * @return 响应
   */
  public static <T> ApiDataResponse<T> error(String message) {
    return new ApiDataResponse<>(ApiStatusEnum.FAIL_APP, message);
  }

  /**
   * 失败响应（指定状态）
   *
   * @param status 状态码
   * @param message 信息（可空）
   * @param <T> 数据类型
   * @return 响应
   */
  public static <T> ApiDataResponse<T> error(int status, String message) {
    return new ApiDataResponse<>(status, message);
  }

  /**
   * 失败响应（指定枚举）
   *
   * @param apiStatusEnum 状态枚举
   * @param message 信息（可空）
   * @param <T> 数据类型
   * @return 响应
   */
  public static <T> ApiDataResponse<T> error(ApiStatusEnum apiStatusEnum, String message) {
    return new ApiDataResponse<>(apiStatusEnum, message);
  }
}
