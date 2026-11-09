package com.rongia.shared.domain.valueobject;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** 值对象抽象基类：通过属性值判等，无唯一标识，通常不可变 */
public abstract class ValueObject implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  /**
   * 返回参与判等的属性
   *
   * @return 属性数组
   */
  protected abstract @Nullable Object[] getEqualityComponents();

  /**
   * 基于属性值判等
   *
   * @param o 比较对象
   * @return 相等返回 true
   */
  @Override
  public final boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ValueObject that = (ValueObject) o;
    return Objects.deepEquals(getEqualityComponents(), that.getEqualityComponents());
  }

  /**
   * 基于属性值计算哈希
   *
   * @return 哈希值
   */
  @Override
  public final int hashCode() {
    return Objects.hash(getEqualityComponents());
  }
}
