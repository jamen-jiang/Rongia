package com.rongia.system.application.serialnumber;

import com.rongia.shared.domain.event.DomainEventHandler;
import com.rongia.shared.domain.event.OrderAddedEvent;
import com.rongia.system.domain.serialnumber.SerialNumberRuleDomainService;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;

/**
 * 订单新增事件监听器：给没带单号的单据分配流水号
 *
 * <p>工厂/领域服务创建单据时单号传空串，落库事务里本监听器按流水号类型编码取号回填；
 * 手工编号的单据（构造时就带了号）也会发 {@link OrderAddedEvent}，这里直接跳过（幂等）。
 * 取号失败（规则没配/停用）抛异常，随业务事务一起回滚——没有规则的单据类型建不了单。
 */
@NullMarked
@Component
public class SerialNumberRuleEventHandler {

  /** 流水号领域服务 */
  private final SerialNumberRuleDomainService ruleDomainService;

  /**
   * 构造器注入
   *
   * @param ruleDomainService 流水号领域服务
   */
  public SerialNumberRuleEventHandler(SerialNumberRuleDomainService ruleDomainService) {
    this.ruleDomainService = ruleDomainService;
  }

  /**
   * 处理订单新增事件：单号为空则取号回填
   *
   * @param event 订单新增事件
   */
  @DomainEventHandler
  public void handle(OrderAddedEvent event) {
    if (!event.getOrder().getNumber().isBlank()) {
      return;
    }
    event.getOrder().assignNumber(ruleDomainService.next(event.getType()));
  }
}
