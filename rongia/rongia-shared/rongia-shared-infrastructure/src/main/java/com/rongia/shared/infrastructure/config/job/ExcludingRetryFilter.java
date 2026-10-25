package com.rongia.shared.infrastructure.config.job;

import java.util.Set;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.filters.RetryFilter;
import org.jobrunr.jobs.states.FailedState;
import org.jobrunr.jobs.states.JobState;

/** 自定义重试过滤器：可指定异常不重试 */
public class ExcludingRetryFilter extends RetryFilter {

  /** 不重试的异常类型 */
  private final Set<Class<? extends Exception>> exceptionsNotToRetry;

  /**
   * 构造器
   *
   * @param numberOfRetries 最大重试次数
   * @param backOffSeed 指数退避种子（秒）
   * @param exceptionsNotToRetry 不重试的异常类型
   */
  @SafeVarargs
  public ExcludingRetryFilter(
      int numberOfRetries, int backOffSeed, Class<? extends Exception>... exceptionsNotToRetry) {
    super(numberOfRetries, backOffSeed);
    this.exceptionsNotToRetry = Set.of(exceptionsNotToRetry);
  }

  @Override
  public void onStateElection(Job job, JobState newState) {
    // 只处理失败状态
    if (newState instanceof FailedState failedState) {
      Exception exception = failedState.getException();
      if (exception != null) {
        for (Class<? extends Exception> exClass : exceptionsNotToRetry) {
          if (exClass.isInstance(exception)) {
            // 命中排除列表，直接返回，不触发父类重试逻辑
            return;
          }
        }
      }
    }
    // 其余情况交给父类（默认重试逻辑）
    super.onStateElection(job, newState);
  }
}
