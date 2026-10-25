package com.rongia.shared.infrastructure.core.job;

import com.rongia.shared.application.port.job.BackgroundJob;
import org.jobrunr.scheduling.JobScheduler;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;

/** 后台任务调度端口实现：基于 JobRunr */
@NullMarked
@Component
public class JobRunrBackgroundJobAdapter implements BackgroundJob {

  /** JobRunr 调度器 */
  private final JobScheduler jobScheduler;

  /** 任务调用器 */
  private final JobInvoker jobInvoker;

  /**
   * 构造器注入
   *
   * @param jobScheduler JobRunr 调度器
   * @param jobInvoker 任务调用器
   */
  public JobRunrBackgroundJobAdapter(JobScheduler jobScheduler, JobInvoker jobInvoker) {
    this.jobScheduler = jobScheduler;
    this.jobInvoker = jobInvoker;
  }

  /**
   * 注册周期性任务（存在则更新）
   *
   * @param id 任务Id
   * @param cron cron 表达式
   * @param beanName 目标 Bean 名称
   * @param methodName 目标方法名
   */
  @Override
  public void scheduleRecurring(String id, String cron, String beanName, String methodName) {
    jobScheduler.scheduleRecurrently(id, cron, () -> jobInvoker.invoke(beanName, methodName));
  }

  /**
   * 删除周期性任务
   *
   * @param id 任务Id
   */
  @Override
  public void deleteRecurring(String id) {
    jobScheduler.deleteRecurringJob(id);
  }

  /**
   * 立即执行一次任务逻辑
   *
   * <p>通过 JobScheduler 实例的 enqueue 方法提交一个一次性任务， 不影响原有的周期性调度。
   *
   * @param beanName 目标 Bean 名称
   * @param methodName 目标方法名
   */
  @Override
  public void triggerOnce(String beanName, String methodName) {
    jobScheduler.enqueue(() -> jobInvoker.invoke(beanName, methodName));
  }
}
