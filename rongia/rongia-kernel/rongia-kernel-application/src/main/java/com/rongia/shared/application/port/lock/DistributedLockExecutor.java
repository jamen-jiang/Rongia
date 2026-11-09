package com.rongia.shared.application.port.lock;

import java.util.function.Supplier;

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
 * <p>两种用法共用的能力：防重入、抢锁失败抛异常、持有者校验、ThreadLocal 清理。
 */
public interface DistributedLockExecutor {

  /**
   * 同步执行（无返回值版本）
   *
   * <p>委托给 {@link #execute(String, Supplier)}，内部把 {@link Runnable} 包一层返回 null。
   *
   * @param key 锁 key
   * @param action 业务逻辑
   */
  void execute(String key, Runnable action);

  /**
   * 同步执行（有返回值版本）
   *
   * <p>执行流程：
   *
   * <ol>
   *   <li>防重入判断：当前线程已持有该 key，直接执行；
   *   <li>获取锁（不等待，抢不到直接抛异常）；
   *   <li>有事务：注册事务完成回调，事务提交 / 回滚后释放；
   *   <li>无事务：finally 里释放。
   * </ol>
   *
   * @param key 锁 key
   * @param action 业务逻辑
   * @param <T> 返回值类型
   * @return 业务逻辑执行结果
   * @throws IllegalStateException 获取锁失败 / 获取锁被中断时抛出
   */
  <T> T execute(String key, Supplier<T> action);

  /**
   * 手动获取锁并返回句柄（异步流场景）
   *
   * <p>用于「锁的生命周期需要跟异步流（MessageStream）对齐」的场景——由调用方决定何时 {@link DistributedLockHandle#release()}。
   *
   * <p>release() 时：
   *
   * <ul>
   *   <li>若当前线程有活跃事务：挂到事务同步器，等事务提交 / 回滚后再真正解锁；
   *   <li>若无事务：立即解锁。
   * </ul>
   *
   * @param key 锁 key
   * @return 锁句柄
   * @throws IllegalStateException 获取锁失败 / 获取锁被中断时抛出
   */
  DistributedLockHandle acquire(String key);
}
