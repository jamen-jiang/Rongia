package com.rongia.shared.domain.aggregate;

import com.rongia.shared.domain.entity.UpdateEntity;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 支持更新审计信息的聚合根基类 记录更新人、更新人名称、更新时间 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@MappedSuperclass
public abstract class UpdateAggregateRoot extends CreateAggregateRoot implements UpdateEntity {

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

  @Override
  public void update(long userId, String userName) {
    this.updatedBy = userId;
    this.updatedByName = userName;
    this.updatedTime = LocalDateTime.now();
  }
}
