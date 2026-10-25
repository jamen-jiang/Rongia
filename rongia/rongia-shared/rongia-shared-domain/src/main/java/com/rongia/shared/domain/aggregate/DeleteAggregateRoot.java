package com.rongia.shared.domain.aggregate;

import com.rongia.shared.domain.entity.DeleteEntity;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.jspecify.annotations.NullMarked;

/** 支持逻辑删除（软删除）的聚合根基类 适用于需要"假删除"功能的业务实体 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@MappedSuperclass
@SQLRestriction("IsDeleted = 0")
@SQLDelete(sql = "UPDATE [%TABLE%] SET [IsDeleted] = 1 WHERE [Id] = ?")
public abstract class DeleteAggregateRoot extends BaseAggregateRoot implements DeleteEntity {

  /** 逻辑删除标识 0 = 未删除，1 = 已删除 */
  @Column(name = "IsDeleted", nullable = false)
  private Boolean isDeleted = false;

/** 标记为已删除（逻辑删除） 子类可重写以触发领域事件 */
  @Override
  public void delete() {
    this.isDeleted = true;
    // 可在此发布领域事件，如 EntityDeletedEvent
  }
}
