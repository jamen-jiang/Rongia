package com.rongia.shared.domain.event;


import com.rongia.shared.domain.aggregate.AggregateRoot;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 添加审计日志事件 */
@NullMarked
@Getter
public class AddAuditLogEvent extends DomainEvent {

  /** 动作 */
  private final String action;

  /** 备注（可空） */
  @Nullable private final String remark;

  /**
   * 构造器
   *
   * @param aggregateRoot 聚合根
   * @param action 动作
   * @param remark 备注（可空）
   */
  public AddAuditLogEvent(AggregateRoot aggregateRoot, String action, @Nullable String remark) {
    super(aggregateRoot);
    this.action = action;
    this.remark = remark;
  }

  /**
   * 构造器（备注为空）
   *
   * @param aggregateRoot 聚合根
   * @param action 动作
   */
  public AddAuditLogEvent(AggregateRoot aggregateRoot, String action) {
    this(aggregateRoot, action, null);
  }
}
