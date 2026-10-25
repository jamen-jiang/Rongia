package com.rongia.shared.application.port.lock;

/**
 * 分布式锁句柄
 *
 * <p>用于异步流场景（如 Axon 拦截器），锁的释放时机由调用方决定。
 */
public interface DistributedLockHandle {

  /** 释放锁 */
  void release();
}
