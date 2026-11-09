package com.rongia.shared.domain.aggregate;

import com.rongia.shared.domain.enums.SerialNumberTypeEnum;
import com.rongia.shared.domain.event.AddAuditLogEvent;
import com.rongia.shared.domain.event.OrderAddedEvent;
import com.rongia.shared.domain.exception.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.Nationalized;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 订单聚合根基类 */
@NullMarked
@Getter
@MappedSuperclass
public abstract class OrderAggregateRoot extends UpdateAggregateRoot {

  /** 单号 */
  @Nationalized
  @Column(name = "Number", nullable = false, length = 100)
  private String number = "";

  /** 无参构造器（供框架反射使用） */
  protected OrderAggregateRoot() {}

  /**
   * 业务创建入口：必须带单号
   *
   * @param number 单号
   */
  protected OrderAggregateRoot(String number) {
    assignNumber(number);
  }

  /**
   * 获取流水号类型
   *
   * @return 本单据在流水号规则里的类型
   */
  public abstract SerialNumberTypeEnum getSerialNumberType();

  /** 分配单号，只能分配一次 */
  public void assignNumber(String number) {
    if (!this.number.isBlank()) {
      throw new DomainException("已分配单号，不能重复分配!");
    }
    this.number = number;
    addDomainEvent(new OrderAddedEvent(this));
  }

  /**
   * @param action 动作
   */
  public void audit(String action) {
    this.audit(action, null);
  }

  /**
   * @param action 动作
   * @param remark 备注
   */
  public void audit(String action, @Nullable String remark) {
    addDomainEvent(new AddAuditLogEvent(this, action, remark));
  }
}
