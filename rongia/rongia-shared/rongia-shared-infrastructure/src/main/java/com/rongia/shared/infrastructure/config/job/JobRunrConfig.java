package com.rongia.shared.infrastructure.config.job;

import com.rongia.shared.application.exception.ApiException;
import com.rongia.shared.domain.exception.DomainException;
import java.util.List;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.jobrunr.configuration.JobRunr;
import org.jobrunr.jobs.filters.JobFilter;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.server.JobActivator;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.common.SqlStorageProviderFactory;
import org.jobrunr.utils.mapper.jackson3.Jackson3JsonMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** JobRunr 手动配置，绕过 Spring Boot 4 自动配置的循环依赖问题 */
@Slf4j
@Configuration
public class JobRunrConfig {

  /** 表前缀，与 application.yml 的 jobrunr.database.table-prefix 一致 */
  private static final String TABLE_PREFIX = "jobrunr_";

  /**
   * 自定义重试过滤器：3 次重试，退避种子 5 秒，ApiException 不重试
   *
   * <p>继承 RetryFilter，注册后 JobRunr 不会再添加默认的 DefaultRetryFilter。
   */
  @Bean
  public JobFilter excludingRetryFilter() {
    return new ExcludingRetryFilter(3, 5, ApiException.class, DomainException.class);
  }

  /** 任务参数序列化，缺了会 NPE */
  @Bean
  public JobMapper jobMapper() {
    return new JobMapper(new Jackson3JsonMapper());
  }

  /**
   * 存储：指定表前缀 + JobMapper
   *
   * @param dataSource 数据源
   * @param jobMapper 任务参数序列化器
   */
  @Bean
  public StorageProvider storageProvider(DataSource dataSource, JobMapper jobMapper) {
    StorageProvider storageProvider = SqlStorageProviderFactory.using(dataSource, TABLE_PREFIX);
    storageProvider.setJobMapper(jobMapper);
    return storageProvider;
  }

  /**
   * ★ JobActivator：让 JobRunr 通过 Spring 容器解析任务里用到的 Bean
   *
   * @param applicationContext Spring 应用上下文
   */
  @Bean
  public JobActivator jobActivator(ApplicationContext applicationContext) {
    return applicationContext::getBean;
  }

  /**
   * 用 Fluent API 创建，不走 Spring Bean，避免循环依赖；同时注册所有 JobFilter + JobActivator
   *
   * @param storageProvider 任务存储提供器
   * @param jobActivator 任务激活器（从 Spring 容器解析任务用到的 Bean）
   * @param jobFilters 任务过滤器列表（含自定义的重试过滤器、上下文过滤器等）
   */
  @Bean
  public JobScheduler jobScheduler(
      StorageProvider storageProvider, JobActivator jobActivator, List<JobFilter> jobFilters) {
    log.info("[JobRunr] 注册 JobFilter 数量: {}", jobFilters.size());
    jobFilters.forEach(f -> log.info("[JobRunr] JobFilter: {}", f.getClass().getName()));

    return JobRunr.configure()
        .useStorageProvider(storageProvider)
        .useJobActivator(jobActivator)
        .useBackgroundJobServer()
        .withJobFilter(jobFilters.toArray(new JobFilter[0]))
        .useDashboard()
        .initialize()
        .getJobScheduler();
  }
}
