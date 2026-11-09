package com.rongia.shared.domain.entity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.io.Serializable;
import java.util.Objects;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 实体抽象基类 */
@NullMarked
@Getter
@MappedSuperclass
public abstract class BaseEntity implements Serializable, Entity {
  /** 主键，未持久化时为 null，由 JPA 生成后回填 */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Nullable
  private Long id;

  /**
   * 主键（对外友好出口，未持久化返回 0，不返回 null）
   *
   * @return 主键，未持久化时为 0
   */
  public Long getId() {
    return id == null ? 0L : id;
  }

  /**
   * 相等性判断：仅当 Id 非空且相等时视为同一实体
   *
   * @param o 比较对象
   * @return true 表示同一实体
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof BaseEntity entity)) {
      return false;
    }
    // 任一方未持久化（Id 为 0），只有引用相同才相等（引用相同已在上方返回）
    if (getId() == 0L || entity.getId() == 0L) {
      return false;
    }
    return Objects.equals(id, entity.id);
  }

  /**
   * 哈希值：Id 为空时用对象地址，避免所有新对象哈希冲突
   *
   * @return 哈希值
   */
  @Override
  public int hashCode() {
    return id == null ? System.identityHashCode(this) : id.hashCode();
  }

  /**
   * 字符串表示
   *
   * @return 类名 + Id
   */
  @Override
  public String toString() {
    return String.format("Entity of type %s with id: %s", this.getClass().getName(), getId());
  }
}
