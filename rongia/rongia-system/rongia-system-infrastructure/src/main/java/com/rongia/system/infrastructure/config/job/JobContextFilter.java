package com.rongia.system.infrastructure.config.job;

import com.rongia.shared.infrastructure.core.http.HttpContext;
import com.rongia.shared.infrastructure.core.http.HttpContextHolder;
import com.rongia.shared.infrastructure.security.user.JobContextHolder;
import com.rongia.shared.infrastructure.security.user.LoginUser;
import com.rongia.shared.infrastructure.security.user.StpLoginUserUtil;
import com.rongia.system.infrastructure.security.user.LoginUserService;
import lombok.extern.slf4j.Slf4j;
import org.jobrunr.jobs.AbstractJob;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.filters.JobClientFilter;
import org.jobrunr.jobs.filters.JobServerFilter;
import org.jobrunr.jobs.states.FailedState;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** 对标 .NET HangfireFilter：任务创建时保存 userId + HttpContext，执行时恢复 */
@Slf4j
@Component
public class JobContextFilter implements JobClientFilter, JobServerFilter {

  /** metadata key：用户 ID */
  private static final String META_USER_ID = "userId";

  /** metadata key：用户名（预留给将来恢复 WebSocket 通知推送，当前无消费者） */
  private static final String META_USER_NAME = "userName";

  /** metadata key：HTTP 上下文（JSON 字符串） */
  private static final String META_HTTP_CONTEXT = "httpContext";

  /** JSON 序列化器，用于 HttpContext 存取 */
  private final JsonMapper jsonMapper;

  /** 登录用户服务，按 userId 加载 LoginUser */
  private final LoginUserService loginUserService;

  /**
   * 构造器注入
   *
   * @param jsonMapper JSON 序列化器
   * @param loginUserService 登录用户服务
   */
  public JobContextFilter(JsonMapper jsonMapper, LoginUserService loginUserService) {
    this.jsonMapper = jsonMapper;
    this.loginUserService = loginUserService;
  }

  // ========== 任务入队时保存上下文（对标 OnCreating） ==========

  /**
   * 任务入队时保存 userId + userName + HttpContext 到 metadata
   *
   * @param job 任务
   */
  @Override
  public void onCreating(AbstractJob job) {
    // 用 instanceof 而不是直接强转：RecurringJob 也是 AbstractJob，但没有 getMetadata
    if (!(job instanceof Job actualJob)) {
      return;
    }

    // 1. 保存 userId + userName（userName 预留给将来恢复通知推送）
    LoginUser user = currentUser();
    if (user != null) {
      actualJob.getMetadata().put(META_USER_ID, user.getUserId());
      actualJob.getMetadata().put(META_USER_NAME, user.getUserName());
      log.debug("[JobRunr] 保存 userId={}, userName={}", user.getUserId(), user.getUserName());
    }

    // 2. 保存 HttpContext
    HttpContext httpContext = HttpContextHolder.getContext();
    if (httpContext != null) {
      try {
        actualJob.getMetadata().put(META_HTTP_CONTEXT, jsonMapper.writeValueAsString(httpContext));
        log.debug("[JobRunr] 保存 HttpContext: apiUrl={}", httpContext.getApiUrl());
      } catch (Exception e) {
        log.warn("[JobRunr] 序列化 HttpContext 失败", e);
      }
    }
  }

  // ========== 任务执行前恢复上下文（对标 OnPerforming） ==========

  /**
   * 任务执行前恢复登录用户上下文和 HttpContext
   *
   * @param job 任务
   */
  @Override
  public void onProcessing(Job job) {
    clearAll();

    // 1. 按 userId 查 LoginUser，绑定到线程上下文（后台线程无请求上下文）
    Object userIdObj = job.getMetadata().get(META_USER_ID);
    if (userIdObj != null) {
      try {
        Long userId = Long.parseLong(userIdObj.toString());
        LoginUser user = loginUserService.loadUserByUserId(userId);
        JobContextHolder.set(user);
        log.debug("[JobRunr] 恢复 LoginUser: userId={}", userId);
      } catch (Exception e) {
        log.warn("[JobRunr] 恢复 LoginUser 失败: userId={}", userIdObj, e);
      }
    }

    // 2. 恢复 HttpContext 到 ThreadLocal
    Object httpJson = job.getMetadata().get(META_HTTP_CONTEXT);
    if (httpJson != null) {
      try {
        HttpContext httpContext = jsonMapper.readValue(httpJson.toString(), HttpContext.class);
        HttpContextHolder.set(httpContext);
        log.debug("[JobRunr] 恢复 HttpContext: apiUrl={}", httpContext.getApiUrl());
      } catch (Exception e) {
        log.warn("[JobRunr] 反序列化 HttpContext 失败", e);
      }
    }
  }

  // ========== 任务成功 ==========

  /**
   * 任务成功后清理上下文（WebSocket 通知已临时移除）
   *
   * @param job 任务
   */
  @Override
  public void onProcessingSucceeded(Job job) {
    clearAll();
  }

  // ========== 任务失败（每次，包括最终那次） ==========

  /**
   * 任务每次失败时记录日志并清理上下文
   *
   * @param job 任务
   * @param e 异常
   */
  @Override
  public void onProcessingFailed(Job job, Exception e) {
    log.debug(
        "[JobRunr] 任务失败: jobId={}, 已失败次数={}",
        job.getId(),
        job.getJobStatesOfType(FailedState.class).count());
    // 必须清：JobRunr 执行线程是复用的，不清会污染下一个任务
    clearAll();
  }

  // ========== 重试耗尽，最终失败 ==========

  /**
   * 重试耗尽：WebSocket 通知已临时移除，暂无额外处理
   *
   * @param job 任务
   */
  @Override
  public void onFailedAfterRetries(Job job) {
    // 通知推送随 WebSocket 功能一起临时移除；恢复时在此补回 pushLog / pushTaskDone
  }

  // ========== 工具 ==========

  /**
   * 取当前请求的登录用户
   *
   * @return 登录用户，无则返回 null
   */
  private LoginUser currentUser() {
    return StpLoginUserUtil.getLoginUser();
  }

  /** 清理登录用户上下文和 HttpContext */
  private void clearAll() {
    JobContextHolder.clear();
    HttpContextHolder.clear();
  }
}
