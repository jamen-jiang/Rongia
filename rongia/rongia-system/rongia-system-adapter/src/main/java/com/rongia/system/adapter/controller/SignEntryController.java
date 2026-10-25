package com.rongia.system.adapter.controller;

import com.rongia.system.application.signentry.command.RefreshTokenCommand;
import com.rongia.system.application.signentry.command.SignInCommand;
import com.rongia.system.application.signentry.command.SignInResponse;
import com.rongia.system.application.signentry.command.SignOutCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import org.axonframework.messaging.commandhandling.gateway.CommandGateway;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 登录入口 */
@Tag(name = "认证管理", description = "登录、刷新令牌、登出")
@RestController
@RequestMapping("/SignEntry")
public class SignEntryController {

  /** 命令网关 */
  private final CommandGateway commandGateway;

  /**
   * 构造器注入
   *
   * @param commandGateway 命令网关
   */
  public SignEntryController(CommandGateway commandGateway) {
    this.commandGateway = commandGateway;
  }

  /**
   * 登录
   *
   * @param command 登录请求
   * @return 登录响应
   */
  @PermitAll
  @Operation(summary = "登录")
  @PostMapping("/SignIn")
  public SignInResponse signIn(@RequestBody SignInCommand command) {
    return commandGateway.sendAndWait(command, SignInResponse.class);
  }

  /**
   * 刷新令牌
   *
   * @param command 刷新令牌请求
   * @return 登录响应
   */
  @PermitAll
  @Operation(summary = "刷新token")
  @PostMapping("/RefreshToken")
  public SignInResponse refreshToken(@RequestBody RefreshTokenCommand command) {
    return commandGateway.sendAndWait(command, SignInResponse.class);
  }

  /**
   * 登出
   *
   * @param command 刷新令牌请求
   */
  @Operation(summary = "登出")
  @PostMapping("/SignOut")
  public void signOut(@RequestBody SignOutCommand command) {
    commandGateway.sendAndWait(command);
  }
}
