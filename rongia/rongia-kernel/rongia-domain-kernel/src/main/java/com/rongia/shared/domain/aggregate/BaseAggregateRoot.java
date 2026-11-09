package com.rongia.shared.domain.aggregate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.rongia.shared.domain.entity.BaseEntity;
import com.rongia.shared.domain.event.DomainEvent;
import org.jspecify.annotations.NullMarked;

/** 聚合根实体基类 */
@NullMarked
public abstract class BaseAggregateRoot extends BaseEntity implements AggregateRoot {
  /** 领域事件列表 */
  private final transient List<DomainEvent> domainEvents = new ArrayList<>();

  /**
   * 获取当前聚合根产生的所有领域事件（只读）
   *
   * @return 领域事件列表
   */
  @Override
  public List<DomainEvent> getDomainEvents() {
    return Collections.unmodifiableList(domainEvents);
  }

  /**
   * 添加一个领域事件
   *
   * @param event 领域事件
   */
  @Override
  public void addDomainEvent(DomainEvent event) {
    domainEvents.add(event);
  }

  /**
   * 移除指定的领域事件
   *
   * @param event 领域事件
   */
  @Override
  public void removeDomainEvent(DomainEvent event) {
    domainEvents.remove(event);
  }

  /** 清除当前聚合根注册的所有领域事件 */
  @Override
  public void clearDomainEvents() {
    domainEvents.clear();
  }
}
