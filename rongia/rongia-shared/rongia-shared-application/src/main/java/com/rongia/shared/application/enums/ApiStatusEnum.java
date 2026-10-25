package com.rongia.shared.application.enums;

import lombok.Getter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Api 状态 */
@NullMarked
@Getter
public enum ApiStatusEnum {

  /** 成功 */
  SUCCESS(200, "成功"),

  /** 令牌无效 */
  FAIL_TOKEN_UNVALID(1, "令牌无效"),

  /** 令牌过期 */
  FAIL_TOKEN_EXPIRED(2, "令牌过期"),

  /** 没访问权限 */
  FAIL_UNAUTHORIZED(401, "没访问权限"),

  /** 403 */
  FAIL_FORBIDDEN(403, "403"),

  /** 应用程序错误 */
  FAIL_APP(98, "应用程序错误"),

  /** 系统异常 */
  FAIL_EXCEPTION(99, "系统异常"),

  /** 应用程序错误提示 */
  FAIL_APP_SHOW_TIP(100, "应用程序错误提示");

  /** 状态码 */
  private final Integer code;

  /** 标签 */
  private final String label;

  /**
   * 构造器
   *
   * @param code 状态码
   * @param label 标签
   */
  ApiStatusEnum(int code, String label) {
    this.code = code;
    this.label = label;
  }

  /**
   * 按状态码查找枚举
   *
   * @param code 状态码
   * @return 对应枚举；找不到返回 null
   */
  @Nullable
  public static ApiStatusEnum fromCode(int code) {
    for (ApiStatusEnum value : values()) {
      if (value.code == code) {
        return value;
      }
    }
    return null;
  }
}
