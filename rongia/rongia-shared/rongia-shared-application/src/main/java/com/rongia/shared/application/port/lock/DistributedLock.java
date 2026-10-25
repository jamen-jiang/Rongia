package com.rongia.shared.application.port.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 分布式锁注解
 *
 * <pre>
 *   // 只用 key
 *   {@code @DistributedLock("'Stock_' + #variantId + '_' + #warehouseId")}
 *
 *   // prefix + key
 *   {@code @DistributedLock(prefix = "Stock", value = "#variantId + '_' + #warehouseId")}
 *
 *   // prefix + key + condition
 *   {@code @DistributedLock(prefix = "Stock", value = "#variantId", condition = "#qty > 0")}
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

  /** 锁 key 前缀，与 value 拼接（用 ":" 分隔） */
  String prefix() default "";

  /** 锁 key 表达式（SpEL），为空时用「类名_方法名」 */
  String value() default "";

  /** 条件表达式（SpEL），为 false 时跳过加锁 */
  String condition() default "";
}
