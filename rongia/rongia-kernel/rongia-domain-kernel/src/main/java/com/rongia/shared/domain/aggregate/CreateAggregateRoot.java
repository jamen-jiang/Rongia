package com.rongia.shared.domain.aggregate;

import com.rongia.shared.domain.entity.CreateEntity;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 支持创建审计信息的聚合根基类 记录创建人、创建人名称、创建时间 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@MappedSuperclass
public abstract class CreateAggregateRoot extends DeleteAggregateRoot implements CreateEntity {

  /** 创建人Id（必填） */
  @Column(name = "CreatedBy", nullable = false)
  private Long createdBy;

  /** 创建人名称（必填，nvarchar(50)） */
  @Column(name = "CreatedByName", nullable = false, length = 50)
  private String createdByName;

  /** 创建时间（必填） */
  @Column(name = "CreatedTime", nullable = false)
  private LocalDateTime createdTime;

  public void create(long userId, String userName, @Nullable LocalDateTime dateTime) {
    this.createdBy = userId;
    this.createdByName = userName;
    this.createdTime = dateTime != null ? dateTime : LocalDateTime.now();
  }

  public void create(long userId, String userName) {
    create(userId, userName, null);
  }
}
