package com.rongia.shared.application.exception;

import com.rongia.shared.application.enums.ApiStatusEnum;
import lombok.Setter;

/** 自定义业务异常 */
public class ApiException extends RuntimeException {

  /** 错误代码 */
  @Setter private int code;

  /** 自定义错误信息 */
  private final String message;

  /** 默认构造器；code 取 Fail_App */
  public ApiException() {
    this.message = null;
  }

  /**
   * 构造器
   *
   * @param code 错误代码
   */
  public ApiException(int code) {
    this.code = code;
    this.message = null;
  }

  /**
   * 构造器
   *
   * @param code 错误代码
   */
  public ApiException(ApiStatusEnum code) {
    this.code = code.getCode();
    this.message = null;
  }

  /**
   * 构造器
   *
   * @param message 错误信息，可空
   */
  public ApiException(String message) {
    super(message);
    this.message = message;
  }

  /**
   * 构造器
   *
   * @param code 错误代码
   * @param message 错误信息，可空
   */
  public ApiException(int code, String message) {
    super(message);
    this.code = code;
    this.message = message;
  }

  /**
   * 构造器
   *
   * @param code 错误代码
   * @param message 错误信息，可空
   */
  public ApiException(ApiStatusEnum code, String message) {
    super(message);
    this.code = code.getCode();
    this.message = message;
  }

  /**
   * 获取错误代码；未设置时返回 FAIL_APP
   *
   * @return 错误代码
   */
  public int getCode() {
    return code == 0 ? ApiStatusEnum.FAIL_APP.getCode() : code;
  }

  /**
   * 获取错误信息；自定义为空时回退到父类的 message
   *
   * @return 错误信息，可空
   */
  @Override
  public String getMessage() {
    return (message == null || message.isEmpty()) ? super.getMessage() : message;
  }
}
