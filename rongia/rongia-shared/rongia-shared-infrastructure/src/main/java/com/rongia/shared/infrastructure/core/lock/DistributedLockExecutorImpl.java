package com.rongia.shared.infrastructure.core.lock;

import com.rongia.shared.application.port.lock.DistributedLockExecutor;
import com.rongia.shared.application.port.lock.DistributedLockHandle;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 分布式锁执行器
 *
 * <p>对外提供两种用法：
 *
 * <ul>
 *   <li>{@link #execute(String, Supplier)} / {@link #execute(String, Runnable)}：同步业务使用，
 *       有事务时锁绑定到事务提交后释放，无事务时方法返回即释放；
 *   <li>{@link #acquire(String)}：返回 {@link DistributedLockHandle}，用于异步流场景（如 Axon
 *       拦截器），由调用方决定释放时机。若调用时已有活跃事务，release() 也会挂到事务提交后。
 * </ul>
 *
 * <p>可重入由 Redisson 的 {@link RLock} 保证：同一线程重复加锁只计数，不阻塞、不多次抢 Redis。
 */
@Slf4j
@Component
public class DistributedLockExecutorImpl implements DistributedLockExecutor {

  /** Redisson 客户端 */
  private final RedissonClient redissonClient;

  /**
   * 构造器注入
   *
   * @param redissonClient Redisson 客户端
   */
  public DistributedLockExecutorImpl(RedissonClient redissonClient) {
    this.redissonClient = redissonClient;
  }

  // ==================== 同步用法 ====================

  /**
   * 同步：无返回值版本
   *
   * @param key 锁 key
   * @param action 业务逻辑
   */
  public void execute(String key, Runnable action) {
    execute(
        key,
        () -> {
          action.run();
          return null;
        });
  }

  /**
   * 同步：有返回值版本
   *
   * <p>执行流程：
   *
   * <ol>
   *   <li>抢锁（Redisson RLock 可重入，同一线程重复加锁只计数）；
   *   <li>有事务：注册事务完成回调，事务提交/回滚后释放；
   *   <li>无事务：finally 里释放。
   * </ol>
   *
   * @param key 锁 key
   * @param action 业务逻辑
   * @param <T> 返回值类型
   * @return 业务逻辑执行结果
   */
  public <T> T execute(String key, Supplier<T> action) {
    // 1. 抢锁（可重入：同一线程已持有时只计数）
    RLock lock = acquireRedisson(key);

    // 2. 有事务：锁绑定到事务，事务完成后释放
    if (isInTransaction()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
              unlock(lock);
            }
          });
      return action.get();
    }

    // 3. 无事务：方法返回即释放
    try {
      return action.get();
    } finally {
      unlock(lock);
    }
  }

  // ==================== 异步流用法 ====================

  /**
   * 异步流场景：手动获取锁并返回句柄，由调用方决定何时 release
   *
   * <p>用于 Axon 拦截器这种「锁的生命周期需要跟 MessageStream 对齐」的场景。
   *
   * <p>release() 时：
   *
   * <ul>
   *   <li>若当前线程有活跃事务：挂到事务同步器，等事务提交/回滚后再真正解锁；
   *   <li>若无事务：立即解锁。
   * </ul>
   *
   * @param key 锁 key
   * @return 锁句柄
   */
  public DistributedLockHandle acquire(String key) {
    // 1. 抢锁（可重入）
    RLock lock = acquireRedisson(key);

    // 2. 返回句柄，release 时根据是否有事务决定释放时机
    return () -> {
      // 2.1 有事务：挂到事务同步器，事务提交/回滚后再真正解锁
      if (isInTransaction()) {
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
              @Override
              public void afterCompletion(int status) {
                unlock(lock);
              }
            });
        return;
      }

      // 2.2 无事务：立即解锁
      unlock(lock);
    };
  }

  // ==================== 私有 ====================

  /**
   * 抢 Redisson 锁（不等待，-1 走看门狗自动续期）
   *
   * @param key 锁 key
   * @return Redisson 锁对象
   */
  private RLock acquireRedisson(String key) {
    RLock lock = redissonClient.getLock(key);
    try {
      if (!lock.tryLock(0, -1, TimeUnit.SECONDS)) {
        throw new IllegalStateException("获取分布式锁失败：" + key);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("获取分布式锁被中断：" + key, e);
    }
    return lock;
  }

  /**
   * 释放锁（校验持有者，避免误删别人的锁）
   *
   * <p>内部 catch 所有异常：释放失败只记日志，不传播，避免影响事务回调。
   *
   * @param lock Redisson 锁
   */
  private void unlock(RLock lock) {
    try {
      if (lock.isHeldByCurrentThread()) {
        lock.unlock();
      }
    } catch (Exception e) {
      // 释放失败：连接断开、锁过期、线程不匹配等
      // 只记日志，绝不抛出
      log.warn("释放分布式锁失败：{}", lock.getName(), e);
    }
  }

  /**
   * 当前线程是否在事务里
   *
   * @return true 表示有活跃事务
   */
  private boolean isInTransaction() {
    return TransactionSynchronizationManager.isActualTransactionActive()
        && TransactionSynchronizationManager.isSynchronizationActive();
  }
}
