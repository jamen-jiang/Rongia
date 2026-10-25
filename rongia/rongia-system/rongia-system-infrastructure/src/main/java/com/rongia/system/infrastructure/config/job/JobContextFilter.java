package com.rongia.system.infrastructure.config.job;

import com.rongia.shared.infrastructure.core.http.HttpContext;
import com.rongia.shared.infrastructure.core.http.HttpContextHolder;
import com.rongia.shared.infrastructure.security.user.LoginUser;
import com.rongia.shared.infrastructure.websocket.NotificationPushService;
import com.rongia.system.infrastructure.security.user.LoginUserService;
import lombok.extern.slf4j.Slf4j;
import org.jobrunr.jobs.AbstractJob;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.filters.JobClientFilter;
import org.jobrunr.jobs.filters.JobServerFilter;
import org.jobrunr.jobs.states.FailedState;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** 对标 .NET HangfireFilter：任务创建时保存 userId + HttpContext，执行时恢复 */
@Slf4j
@Component
public class JobContextFilter implements JobClientFilter, JobServerFilter {

  /** metadata key：用户 ID */
  private static final String META_USER_ID = "userId";

  /** metadata key：用户名（onFailedAfterRetries 里上下文已清，用它兜底） */
  private static final String META_USER_NAME = "userName";

  /** metadata key：HTTP 上下文（JSON 字符串） */
  private static final String META_HTTP_CONTEXT = "httpContext";

  /** 消息推送服务 */
  private final NotificationPushService pushService;

  /** JSON 序列化器，用于 HttpContext 存取 */
  private final JsonMapper jsonMapper;

  /** 登录用户服务，按 userId 加载 LoginUser */
  private final LoginUserService loginUserService;

  /**
   * 构造器注入
   *
   * @param pushService 消息推送服务
   * @param jsonMapper JSON 序列化器
   * @param loginUserService 登录用户服务
   */
  public JobContextFilter(
      NotificationPushService pushService,
      JsonMapper jsonMapper,
      LoginUserService loginUserService) {
    this.pushService = pushService;
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

    // 1. 保存 userId + userName（userName 预存，供 onFailedAfterRetries 使用）
    LoginUser user = currentUser();
    if (user != null) {
      actualJob.getMetadata().put(META_USER_ID, user.getUserId());
      actualJob.getMetadata().put(META_USER_NAME, user.getUsername());
      log.debug("[JobRunr] 保存 userId={}, userName={}", user.getUserId(), user.getUsername());
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
   * 任务执行前恢复 SecurityContext 和 HttpContext
   *
   * @param job 任务
   */
  @Override
  public void onProcessing(Job job) {
    clearAll();

    // 1. 按 userId 查 LoginUser，塞进 SecurityContext
    Object userIdObj = job.getMetadata().get(META_USER_ID);
    if (userIdObj != null) {
      try {
        Long userId = Long.parseLong(userIdObj.toString());
        LoginUser user = loginUserService.loadUserByUserId(userId);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        SecurityContextHolder.setContext(context);
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
   * 任务成功后推送完成通知，并清理上下文
   *
   * @param job 任务
   */
  @Override
  public void onProcessingSucceeded(Job job) {
    try {
      String userName = currentUserName();
      if (userName != null) {
        pushService.pushTaskDone(userName, job.getId().toString());
      }
    } finally {
      clearAll();
    }
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
   * 重试耗尽后推送失败通知
   *
   * @param job 任务
   */
  @Override
  public void onFailedAfterRetries(Job job) {
    // onProcessingFailed 已经清过上下文了，这里只能从 metadata 取 userName
    Object userNameObj = job.getMetadata().get(META_USER_NAME);
    if (userNameObj != null) {
      String userName = userNameObj.toString();
      String jobName = job.getJobName();
      pushService.pushLog(userName, "任务失败: " + jobName + "(" + job.getId() + ")", false);
      pushService.pushTaskDone(userName, job.getId().toString());
    }
  }

  // ========== 工具 ==========

  /**
   * 取当前请求的登录用户
   *
   * @return 登录用户，无则返回 null
   */
  private LoginUser currentUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return (auth != null && auth.getPrincipal() instanceof LoginUser u) ? u : null;
  }

  /**
   * 取当前请求的用户名
   *
   * @return 用户名，无则返回 null
   */
  private String currentUserName() {
    LoginUser user = currentUser();
    return user != null ? user.getUsername() : null;
  }

  /** 清理 SecurityContext 和 HttpContext */
  private void clearAll() {
    SecurityContextHolder.clearContext();
    HttpContextHolder.clear();
  }
}
