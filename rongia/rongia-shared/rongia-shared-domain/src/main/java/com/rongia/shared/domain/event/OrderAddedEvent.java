package com.rongia.shared.domain.event;


import com.rongia.shared.domain.aggregate.OrderAggregateRoot;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/** 订单新增事件（用于生成流水号） */
@NullMarked
@Getter
public class OrderAddedEvent extends DomainEvent {

  /** 订单 */
  private final OrderAggregateRoot order;

  /**
   * 构造器
   *
   * @param order 订单
   */
  public OrderAddedEvent(OrderAggregateRoot order) {
    super(order);
    this.order = order;
  }

  /**
   * 流水号类型编码（对应 {@code SerialNumberTypeEnum#getCode()}，流水号规则按它取前缀）
   *
   * <p>到用的时候才问单据，不在这里存字符串：本事件是基类构造器里建出来的，那会儿子类自己的字段还没归位，
   * 类型又是由单据自己的字段算出来的。事件是 save 之后才发布的，那时单据已经装配齐。
   *
   * @return 流水号类型编码
   */
  public String getType() {
    return order.getSerialNumberType().getCode();
  }
}
