package com.rongia.shared.domain.aggregate;

import java.util.List;
import com.rongia.shared.domain.entity.Entity;
import com.rongia.shared.domain.event.DomainEvent;
import org.jspecify.annotations.NullMarked;

/** 聚合根接口 */
@NullMarked
public interface AggregateRoot extends Entity {
  /**
   * 获取当前聚合根产生的所有领域事件（只读）
   *
   * @return 领域事件列表（只读）
   */
  List<DomainEvent> getDomainEvents();

  /**
   * 添加领域事件，将在工作单元提交时分发
   *
   * @param event 领域事件
   */
  void addDomainEvent(DomainEvent event);

  /**
   * 移除指定领域事件
   *
   * @param event 领域事件
   */
  void removeDomainEvent(DomainEvent event);

  /** 清除所有领域事件 */
  void clearDomainEvents();
}
