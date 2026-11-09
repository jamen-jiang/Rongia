package com.rongia.shared.application.port.job;

import org.jspecify.annotations.NullMarked;

/** 后台任务调度端口：封装底层调度框架，application 层只依赖此接口 */
@NullMarked
public interface BackgroundJob {

  /**
   * 注册周期性任务（存在则更新）
   *
   * @param id 任务Id（作为调度框架内的唯一标识）
   * @param cron cron 表达式
   * @param beanName 目标 Bean 名称
   * @param methodName 目标方法名
   */
  void scheduleRecurring(String id, String cron, String beanName, String methodName);

  /**
   * 删除周期性任务
   *
   * @param id 任务Id
   */
  void deleteRecurring(String id);

  /**
   * 立即执行一次任务逻辑（不影响原有的周期性调度）
   *
   * @param beanName 目标 Bean 名称
   * @param methodName 目标方法名
   */
  void triggerOnce(String beanName, String methodName);
}
