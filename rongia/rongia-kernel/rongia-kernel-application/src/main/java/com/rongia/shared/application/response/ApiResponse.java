package com.rongia.shared.application.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
public class ApiResponse<T> {

  /** 状态码 */
  private final Integer code;

  /** 信息（可空） */
  private @Nullable String message;

  /** 结果集（可空） */
  private @Nullable T data;

  /** 默认构造器；状态码取 SUCCESS */
  public ApiResponse() {
    this.code = ApiStatusEnum.SUCCESS.getCode();
  }

  /**
   * 构造器
   *
   * @param data 结果集（可空）
   */
  public ApiResponse(T data) {
    this.data = data;
    this.code = ApiStatusEnum.SUCCESS.getCode();
  }

  /**
   * 构造器
   *
   * @param message 信息（可空）
   */
  public ApiResponse(String message) {
    this.message = message;
    this.code = ApiStatusEnum.SUCCESS.getCode();
  }

  /**
   * 构造器
   *
   * @param apiStatusEnum 状态枚举
   * @param message 信息（可空）
   */
  public ApiResponse(ApiStatusEnum apiStatusEnum, String message) {
    this.code = apiStatusEnum.getCode();
    this.message = message;
  }

  /**
   * 构造器
   *
   * @param code 状态码
   * @param message 信息（可空）
   */
  public ApiResponse(int code, String message) {
    this.code = code;
    this.message = message;
  }

  /**
   * 获取信息；自定义为空时回退到状态枚举的描述
   *
   * @return 信息（可空）
   */
  public String getMessage() {
    if (message == null || message.isBlank()) {
      ApiStatusEnum statusEnum = ApiStatusEnum.fromCode(code);
      return statusEnum == null ? "" : statusEnum.getLabel();
    }
    return message;
  }

  /**
   * 是否成功（不序列化进响应：code 已表达成败）
   *
   * @return true 表示成功
   */
  @JsonIgnore
  public boolean isSuccess() {
    return code.equals(ApiStatusEnum.SUCCESS.getCode());
  }

  // ---------- 静态工厂方法 ----------

  /**
   * 成功响应（无数据）
   *
   * @param <T> 数据类型
   * @return 响应
   */
  public static <T> ApiResponse<T> success() {
    return new ApiResponse<>();
  }

  /**
   * 成功响应（带数据）
   *
   * @param data 数据（可空）
   * @param <T> 数据类型
   * @return 响应
   */
  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>(data);
  }

  /**
   * 成功响应（带信息）
   *
   * @param message 信息（可空）
   * @param <T> 数据类型
   * @return 响应
   */
  public static <T> ApiResponse<T> successMessage(String message) {
    return new ApiResponse<>(message);
  }

  /**
   * 失败响应（默认 FAIL）
   *
   * @param message 信息（可空）
   * @param <T> 数据类型
   * @return 响应
   */
  public static <T> ApiResponse<T> error(String message) {
    return new ApiResponse<>(ApiStatusEnum.FAIL, message);
  }

  /**
   * 失败响应（指定状态）
   *
   * @param code 状态码
   * @param message 信息（可空）
   * @param <T> 数据类型
   * @return 响应
   */
  public static <T> ApiResponse<T> error(int code, String message) {
    return new ApiResponse<>(code, message);
  }

  /**
   * 失败响应（指定枚举）
   *
   * @param apiStatusEnum 状态枚举
   * @param message 信息（可空）
   * @param <T> 数据类型
   * @return 响应
   */
  public static <T> ApiResponse<T> error(ApiStatusEnum apiStatusEnum, String message) {
    return new ApiResponse<>(apiStatusEnum, message);
  }
}
