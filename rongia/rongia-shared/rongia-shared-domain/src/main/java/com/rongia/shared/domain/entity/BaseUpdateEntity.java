package com.rongia.shared.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 支持更新审计信息的领域实体基类 记录更新人、更新人名称、更新时间 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@MappedSuperclass
public abstract class BaseUpdateEntity extends BaseCreateEntity implements UpdateEntity {

  /** 更新人Id（可空） */
  @Column(name = "UpdatedBy")
  @Nullable
  private Long updatedBy;

  /** 更新人名称（可空，nvarchar(50)） */
  @Nationalized
  @Column(name = "UpdatedByName", length = 50)
  @Nullable
  private String updatedByName;

  /** 更新时间（可空） */
  @Column(name = "UpdatedTime")
  @Nullable
  private LocalDateTime updatedTime;

  /**
   * 设置更新审计信息 子类可重写以触发领域事件
   *
   * @param userId 更新人Id
   * @param userName 更新人名称
   */
  @Override
  public void update(long userId, String userName) {
    this.updatedBy = userId;
    this.updatedByName = userName;
    this.updatedTime = LocalDateTime.now();
  }
}
