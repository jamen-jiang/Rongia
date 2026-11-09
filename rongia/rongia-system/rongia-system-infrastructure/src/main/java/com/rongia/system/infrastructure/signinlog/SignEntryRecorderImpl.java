package com.rongia.system.infrastructure.signinlog;

import com.rongia.system.application.signentry.port.SignEntryRecord;
import com.rongia.system.application.signentry.port.SignEntryRecorder;
import com.rongia.system.application.signinlog.command.AddSignInLogCommand;
import com.rongia.system.domain.signinlog.SignEntryTypeEnum;
import com.rongia.kernel.application.port.job.BackgroundExecutor;
import com.rongia.kernel.infrastructure.http.HttpContext;
import com.rongia.kernel.infrastructure.http.HttpContextHolder;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

/**
 * 登录入口日志记录器实现：负责补全 Web 上下文信息，并通过后台执行器异步发送日志命令
 * （不阻塞主流程）
 */
@Component
public class SignEntryRecorderImpl implements SignEntryRecorder {

  /** 后台执行器端口 */
  private final BackgroundExecutor backgroundExecutor;

  /**
   * 构造器注入
   *
   * @param backgroundExecutor 后台执行器端口
   */
  public SignEntryRecorderImpl(BackgroundExecutor backgroundExecutor) {
    this.backgroundExecutor = backgroundExecutor;
  }

  /**
   * 记录一条登录入口日志
   *
   * @param record 日志内容
   */
  @Override
  public void record(@NonNull SignEntryRecord record) {

    // 默认 Web 信息为 null
    String ip = null;
    String userAgent = null;
    String browser = null;
    String os = null;
    String token = record.token();

    // 存在 Web 上下文时才填充（后台任务、定时任务等场景可能为 null）
    HttpContext httpContext = HttpContextHolder.getContext();
    if (httpContext != null) {
      ip = httpContext.getIp();
      userAgent = httpContext.getUserAgent();
      browser = httpContext.getBrowser();
      os = httpContext.getOs();
      // 登出场景请求头未必带令牌，退化为取上下文中已解析出的令牌
      if (SignEntryTypeEnum.SIGN_OUT.getCode().equals(record.type())) {
        token = httpContext.getToken();
      }
    }

    // 组装日志命令：record 的 userName 落到聚合的 userCode（登录账号），givenName 落到 userName（用户名称）
    AddSignInLogCommand command =
        new AddSignInLogCommand(
            record.type(),
            record.userId(),
            record.userName(),
            record.givenName(),
            record.startedOn(),
            record.endedOn(),
            record.elapsedMilliseconds(),
            token,
            ip,
            null,
            userAgent,
            browser,
            os,
            record.success(),
            record.errorMessage(),
            record.exception());

    // 通过后台执行器异步发送命令（不阻塞主流程）
    backgroundExecutor.dispatch(command);
  }
}
