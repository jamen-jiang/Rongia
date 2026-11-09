package com.rongia.shared.domain.util;

import com.rongia.shared.domain.enums.BaseEnum;
import com.rongia.shared.domain.exception.DomainException;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** 枚举工具 */
public final class EnumUtils {

  private EnumUtils() {}

  /**
   * 按编码查找枚举
   *
   * @param enumClass 枚举类型
   * @param code 编码
   * @param <E> 枚举类型
   * @return 枚举（可空），不存在返回 null
   */
  @Nullable
  public static <E extends Enum<E> & BaseEnum> E fromCode(
      @NonNull Class<E> enumClass, @NonNull String code) {
    for (E e : enumClass.getEnumConstants()) {
      if (e.getCode().equals(code)) {
        return e;
      }
    }
    return null;
  }

  /**
   * 校验并规范化枚举编码：去首尾空格后必须能在枚举里找到
   *
   * <p>落库前统一走这一步：空串、乱填的编码当场报错，存进列里的永远是去掉空格的标准编码。
   *
   * @param enumClass 枚举类型
   * @param code 编码（可空）
   * @param subject 报错短语，例如「菜单的类型」，拼成的提示是「菜单的类型xxx有误!」
   * @param <E> 枚举类型
   * @return 去除首尾空格后的编码
   * @throws DomainException 编码为空或不在枚举范围内时抛出
   */
  public static <E extends Enum<E> & BaseEnum> String getCode(
      @NonNull Class<E> enumClass, @Nullable String code, String subject) {
    String trimmed = StringUtils.trimToNull(code);
    if (trimmed == null || fromCode(enumClass, trimmed) == null) {
      throw new DomainException(subject + code + "有误!");
    }
    return trimmed;
  }
}
