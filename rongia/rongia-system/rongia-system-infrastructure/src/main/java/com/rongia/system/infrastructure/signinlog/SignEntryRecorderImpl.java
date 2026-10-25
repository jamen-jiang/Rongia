package com.rongia.system.infrastructure.signinlog;

import com.rongia.system.application.signentry.port.SignEntryRecorder;
import com.rongia.system.application.signinlog.command.AddSignInLogCommand;
import com.rongia.system.domain.signinlog.SignEntryTypeEnum;
import com.rongia.shared.application.port.job.BackgroundExecutor;
import com.rongia.shared.infrastructure.core.http.HttpContext;
import com.rongia.shared.infrastructure.core.http.HttpContextHolder;
import java.time.LocalDateTime;
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
   * @param type 类型：SignIn / RefreshToken / SignOut
   * @param userId 用户Id（可空）
   * @param userName 输入的用户名（可空）
   * @param givenName 用户昵称（可空）
   * @param startedOn 开始时间
   * @param endedOn 结束时间
   * @param elapsedMilliseconds 耗时（毫秒）
   * @param accessToken 访问令牌（可空）
   * @param refreshToken 刷新令牌（可空）
   * @param oldRefreshToken 旧刷新令牌（可空，仅刷新令牌场景使用）
   * @param success 是否成功
   * @param errorMessage 错误信息（可空）
   * @param exception 异常信息（可空）
   */
  @Override
  public void record(
      @NonNull String type,
      Long userId,
      String userName,
      String givenName,
      @NonNull LocalDateTime startedOn,
      @NonNull LocalDateTime endedOn,
      long elapsedMilliseconds,
      String accessToken,
      String refreshToken,
      String oldRefreshToken,
      boolean success,
      String errorMessage,
      String exception) {

    // 默认 Web 信息为 null
    String ip = null;
    String userAgent = null;
    String browser = null;
    String os = null;
    String token = accessToken;

    // 存在 Web 上下文时才填充（后台任务、定时任务等场景可能为 null）
    HttpContext httpContext = HttpContextHolder.getContext();
    if (httpContext != null) {
      ip = httpContext.getIp();
      userAgent = httpContext.getUserAgent();
      browser = httpContext.getBrowser();
      os = httpContext.getOs();
      // 登出场景请求头未必带 accessToken，退化为取上下文中已解析出的令牌
      if (SignEntryTypeEnum.SIGN_OUT.getCode().equals(type)) {
        token = httpContext.getToken();
      }
    }

    // 组装日志命令：接口的 userName 落到聚合的 userCode（登录账号），givenName 落到 userName（用户名称）
    AddSignInLogCommand command =
        new AddSignInLogCommand(
            type,
            userId,
            userName,
            givenName,
            startedOn,
            endedOn,
            elapsedMilliseconds,
            token,
            refreshToken,
            oldRefreshToken,
            ip,
            null,
            userAgent,
            browser,
            os,
            success,
            errorMessage,
            exception);

    // 通过后台执行器异步发送命令（不阻塞主流程）
    backgroundExecutor.dispatch(command);
  }
}
