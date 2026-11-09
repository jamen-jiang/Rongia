package com.rongia.shared.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.jspecify.annotations.NullMarked;

/** 支持逻辑删除（软删除）的实体基类 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@MappedSuperclass
@SQLRestriction("IsDeleted = 0")
@SQLDelete(sql = "UPDATE [%TABLE%] SET [IsDeleted] = 1 WHERE [Id] = ?")
public abstract class BaseDeleteEntity extends BaseEntity implements DeleteEntity {

  /** 逻辑删除标识 0 = 未删除，1 = 已删除 */
  @Column(name = "IsDeleted", nullable = false)
  private Boolean isDeleted = false;

/** 标记为已删除（逻辑删除） */
  @Override
  public void delete() {
    this.isDeleted = true;
  }
}
