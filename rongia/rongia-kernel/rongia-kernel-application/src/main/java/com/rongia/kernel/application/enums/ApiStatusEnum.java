package com.rongia.kernel.application.enums;

import lombok.Getter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Api 状态 */
@NullMarked
@Getter
public enum ApiStatusEnum {

  /** 成功 */
  SUCCESS(200, "成功"),

  /** 失败（业务错误） */
  FAIL(400, "失败"),

  /** 未登录 / 令牌失效（过期、被顶下线等具体原因用 message 区分） */
  UNAUTHORIZED(401, "未登录"),

  /** 没访问权限 */
  FORBIDDEN(403, "没访问权限"),

  /** 系统异常 */
  ERROR(500, "系统异常");

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
