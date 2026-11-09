package com.rongia.shared.infrastructure.core.event;

import com.rongia.shared.domain.aggregate.AggregateRoot;
import com.rongia.shared.domain.event.DomainEvent;
import com.rongia.shared.domain.event.DomainEventPublisher;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/** 拦截 Spring Data JPA 的 save，返回后立即发布领域事件（事务内） */
@Aspect
@Component
public class DomainEventPublishingAspect {

  /** 领域事件发布器 */
  private final DomainEventPublisher eventPublisher;

  /**
   * 构造器注入
   *
   * @param eventPublisher 领域事件发布器
   */
  public DomainEventPublishingAspect(DomainEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  /**
   * save 返回后发布事件（事务未结束）
   *
   * @param saved 已保存的实体
   */
  @AfterReturning(
      pointcut = "execution(* org.springframework.data.repository.CrudRepository+.save(..))",
      returning = "saved")
  public void afterSave(Object saved) {
    publishEvents(saved);
  }

  /**
   * saveAll 返回后发布事件（事务未结束）
   *
   * @param savedList 已保存的实体列表
   */
  @AfterReturning(
      pointcut = "execution(* org.springframework.data.repository.CrudRepository+.saveAll(..))",
      returning = "savedList")
  public void afterSaveAll(Iterable<?> savedList) {
    savedList.forEach(this::publishEvents);
  }

  /**
   * 从聚合根收集并发布领域事件
   *
   * @param entity 实体
   */
  private void publishEvents(Object entity) {
    if (!(entity instanceof AggregateRoot aggregate)) {
      return;
    }
    List<DomainEvent> events = aggregate.getDomainEvents();
    if (events.isEmpty()) {
      return;
    }
    List<DomainEvent> toPublish = new ArrayList<>(events);
    toPublish.sort(Comparator.comparing(DomainEvent::getTimestamp));
    aggregate.clearDomainEvents();
    toPublish.forEach(eventPublisher::publish);
  }
}
