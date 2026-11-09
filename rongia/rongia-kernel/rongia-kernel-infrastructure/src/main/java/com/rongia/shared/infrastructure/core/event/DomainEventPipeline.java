package com.rongia.shared.infrastructure.core.event;

import com.rongia.shared.domain.event.DomainEvent;
import com.rongia.shared.domain.event.DomainEventHandler;
import com.rongia.shared.domain.event.DomainEventPublisher;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jspecify.annotations.NullMarked;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.OrderUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

/**
 * 领域事件分发器。
 *
 * <p>启动时扫描所有 bean 中带 {@link DomainEventHandler} 的方法，建立「事件类型 → 方法」映射。 发布时同步顺序调用所有匹配的方法，异常直接抛出触发事务回滚。
 *
 * <p>用 {@link AopUtils#getTargetClass} 读注解，用 {@link AopUtils#selectInvocableMethod} 拿代理类上的方法， 确保
 * {@code @Transactional} / {@code @DistributedLock} 等 AOP 注解生效。
 *
 * <p><b>契约：</b>
 *
 * <ul>
 *   <li>监听方法必须只有一个 {@link DomainEvent} 子类参数，否则启动失败
 *   <li>同一事件可注册多个监听器，按 {@link Ordered} 顺序执行；未标注时按最低优先级处理
 *   <li>任一监听器抛异常会立即终止后续监听器并向上抛出，触发外层事务回滚
 *   <li>监听器应保证幂等；非事务性副作用（发邮件、调外部 API）需自行补偿
 * </ul>
 */
@NullMarked
@Component
public class DomainEventPipeline
    implements DomainEventPublisher, ApplicationListener<ContextRefreshedEvent> {

  /** Spring 上下文，用于启动时扫描所有 bean */
  private final ApplicationContext ctx;

  /** 事件类型 → 监听方法列表（按 order 排序） */
  private final Map<Class<?>, List<Invoker>> registry = new ConcurrentHashMap<>();

  /** 是否已完成扫描（只扫一次） */
  private final AtomicBoolean initialized = new AtomicBoolean(false);

  /**
   * 构造器注入
   *
   * @param ctx Spring 上下文
   */
  public DomainEventPipeline(ApplicationContext ctx) {
    this.ctx = ctx;
  }

  /**
   * 所有单例 bean 初始化完成后扫描一次，建立注册表
   *
   * @param event 上下文刷新事件
   */
  @Override
  public void onApplicationEvent(ContextRefreshedEvent event) {
    if (!initialized.compareAndSet(false, true)) {
      return;
    }

    for (Object bean : ctx.getBeansOfType(Object.class).values()) {
      Class<?> targetClass = AopUtils.getTargetClass(bean);
      int order = OrderUtils.getOrder(targetClass, Ordered.LOWEST_PRECEDENCE);

      // 遍历当前类及父类声明的所有方法，避免漏掉继承来的监听方法
      for (Method method : ReflectionUtils.getAllDeclaredMethods(targetClass)) {
        if (!method.isAnnotationPresent(DomainEventHandler.class)) {
          continue;
        }
        if (method.getParameterCount() != 1) {
          throw new IllegalStateException("@DomainEventHandler 方法必须只有一个参数: " + method);
        }
        Class<?> eventType = method.getParameterTypes()[0];
        if (!DomainEvent.class.isAssignableFrom(eventType)) {
          throw new IllegalStateException("@DomainEventHandler 参数必须是 DomainEvent 子类: " + method);
        }
        // 从代理类上取可调用的方法，保证 AOP 生效
        Method invocable = AopUtils.selectInvocableMethod(method, bean.getClass());
        invocable.setAccessible(true);
        registry
            .computeIfAbsent(eventType, k -> new ArrayList<>())
            .add(new Invoker(bean, invocable, order));
      }
    }

    // 每个事件类型的监听器按 order 升序，保证执行顺序稳定
    registry.values().forEach(list -> list.sort(Comparator.comparingInt(Invoker::order)));
  }

  /**
   * 发布事件：沿事件类型的继承链查找监听器，同步顺序执行。
   *
   * <p>只遍历事件类及其父类，不遍历整个注册表，性能与事件类型总数无关。
   *
   * @param event 领域事件
   */
  @Override
  public void publish(DomainEvent event) {
    Class<?> type = event.getClass();
    while (type != null && DomainEvent.class.isAssignableFrom(type)) {
      List<Invoker> invokers = registry.get(type);
      if (invokers != null) {
        for (Invoker invoker : invokers) {
          invoker.invoke(event);
        }
      }
      type = type.getSuperclass();
    }
  }

  /**
   * 方法调用封装。
   *
   * @param bean 监听器实例（Spring 代理）
   * @param method 代理类上的可调用方法
   * @param order 执行顺序，值越小越先执行
   */
  private record Invoker(Object bean, Method method, Integer order) {

    /**
     * 反射调用监听方法，异常直接抛出触发事务回滚
     *
     * @param event 领域事件
     */
    void invoke(DomainEvent event) {
      try {
        method.invoke(bean, event);
      } catch (InvocationTargetException e) {
        // 反射调用包装异常，取出真实异常继续抛
        Throwable target = e.getTargetException();
        if (target instanceof RuntimeException re) {
          throw re;
        }
        if (target instanceof Error err) {
          throw err;
        }
        throw new RuntimeException(target);
      } catch (IllegalAccessException e) {
        throw new RuntimeException("事件监听调用失败: " + method, e);
      }
    }
  }
}
