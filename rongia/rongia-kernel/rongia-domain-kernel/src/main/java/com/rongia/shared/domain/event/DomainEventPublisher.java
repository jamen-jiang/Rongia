package com.rongia.shared.domain.event;

import org.jspecify.annotations.NullMarked;

/** 领域事件发布器接口。 Domain 层只依赖这个抽象，不感知 Spring、Pipelinr 等任何框架。 */
@NullMarked
public interface DomainEventPublisher {
  void publish(DomainEvent event);
}
