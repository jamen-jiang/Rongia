package com.rongia.shared.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 支持创建审计信息的领域实体基类 记录创建人、创建人名称、创建时间 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@MappedSuperclass
public abstract class BaseCreateEntity extends BaseDeleteEntity implements CreateEntity {

  /** 创建人Id（必填） */
  @Column(name = "CreatedBy", nullable = false)
  private Long createdBy;

  /** 创建人名称（必填，nvarchar(50)） */
  @Column(name = "CreatedByName", nullable = false, length = 50)
  private String createdByName;

  /** 创建时间（必填） */
  @Column(name = "CreatedTime", nullable = false)
  private LocalDateTime createdTime;

  /**
   * 设置创建审计信息 子类可重写以触发领域事件
   *
   * @param userId 创建人Id
   * @param userName 创建人名称
   * @param dateTime 创建时间（可空，为空时取当前时间）
   */
  public void create(long userId, String userName, @Nullable LocalDateTime dateTime) {
    this.createdBy = userId;
    this.createdByName = userName;
    this.createdTime = dateTime != null ? dateTime : LocalDateTime.now();
    // 可在此发布领域事件，如 EntityCreatedEvent
  }

  /**
   * 设置创建审计信息（使用当前时间）
   *
   * @param userId 创建人 ID
   * @param userName 创建人名称
   */
  public void create(long userId, String userName) {
    create(userId, userName, null);
  }
}
