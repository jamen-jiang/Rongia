package com.rongia.shared.application.response;

import com.rongia.shared.application.enums.ApiStatusEnum;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 统一响应包装 */
@NullMarked
@Getter
public class ApiResponse {

  /** 状态码 */
  private final Integer status;

  /** 信息（可空） */
  private @Nullable String message;

  /** 结果集（可空） */
  private @Nullable Object data;

  /** 默认构造器；状态码取 SUCCESS */
  public ApiResponse() {
    this.status = ApiStatusEnum.SUCCESS.getCode();
  }

  /**
   * 构造器
   *
   * @param data 结果集（可空）
   */
  public ApiResponse(Object data) {
    this.data = data;
    this.status = ApiStatusEnum.SUCCESS.getCode();
  }

  /**
   * 构造器
   *
   * @param message 信息（可空）
   */
  public ApiResponse(String message) {
    this.message = message;
    this.status = ApiStatusEnum.SUCCESS.getCode();
  }

  /**
   * 构造器
   *
   * @param apiStatusEnum 状态枚举
   * @param message 信息（可空）
   */
  public ApiResponse(ApiStatusEnum apiStatusEnum, String message) {
    this.status = apiStatusEnum.getCode();
    this.message = message;
  }

  /**
   * 构造器
   *
   * @param status 状态码
   * @param message 信息（可空）
   */
  public ApiResponse(int status, String message) {
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
  public Boolean getIsSuccess() {
    return status == ApiStatusEnum.SUCCESS.getCode();
  }
}
