package com.rongia.shared.domain.event;

import java.time.LocalDateTime;

import com.rongia.shared.domain.aggregate.AggregateRoot;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/** 领域事件基类 */
@NullMarked
@Getter
public abstract class DomainEvent {

  /** 事件类型（类名） */
  private final String eventType;

  /** 时间戳 */
  private final LocalDateTime timestamp;

  /** 聚合根 */
  private final AggregateRoot aggregateRoot;

  /**
   * 构造器
   *
   * @param aggregateRoot 聚合根
   */
  protected DomainEvent(AggregateRoot aggregateRoot) {
    this.aggregateRoot = aggregateRoot;
    this.eventType = getClass().getSimpleName();
    this.timestamp = LocalDateTime.now();
  }
}
