package com.rongia.shared.domain.event;

import java.lang.annotation.*;

/** 领域事件监听注解，打在方法上。 方法必须只有一个参数，且参数类型是 DomainEvent 的子类。 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DomainEventHandler {}
