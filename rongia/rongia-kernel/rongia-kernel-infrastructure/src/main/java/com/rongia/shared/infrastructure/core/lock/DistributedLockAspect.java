package com.rongia.shared.infrastructure.core.lock;

import com.rongia.shared.application.port.lock.DistributedLock;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 分布式锁切面 */
@Aspect
@Component
@Order(0)
public class DistributedLockAspect {

  /** 分布式锁执行器 */
  private final DistributedLockExecutorImpl executor;

  /**
   * 构造器注入
   *
   * @param executor 分布式锁执行器
   */
  public DistributedLockAspect(DistributedLockExecutorImpl executor) {
    this.executor = executor;
  }

  /**
   * 环绕通知：加锁执行业务
   *
   * @param pjp 切点
   * @param lock 锁注解
   * @return 业务返回值
   * @throws Throwable 业务异常
   */
  @Around("@annotation(lock)")
  public Object around(ProceedingJoinPoint pjp, DistributedLock lock) throws Throwable {
    // 1. condition 不满足：不加锁
    if (!LockKeyResolver.evaluateCondition(pjp, lock.condition())) {
      return pjp.proceed();
    }

    // 2. 解析 key + prefix
    String key = LockKeyResolver.resolve(pjp, lock.value());
    if (!lock.prefix().isBlank()) {
      key = lock.prefix() + ":" + key;
    }

    // 3. 加锁执行业务
    return executor.execute(
        key,
        () -> {
          try {
            return pjp.proceed();
          } catch (RuntimeException | Error e) {
            throw e;
          } catch (Throwable e) {
            throw new RuntimeException(e);
          }
        });
  }
}
