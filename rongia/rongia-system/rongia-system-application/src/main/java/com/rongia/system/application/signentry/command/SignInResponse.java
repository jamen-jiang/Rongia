package com.rongia.system.application.signentry.command;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.NullMarked;

/**
 * 登录响应
 *
 * @param token 访问令牌
 * @param refreshToken 刷新令牌
 */
@NullMarked
@Schema(description = "登录响应")
public record SignInResponse(
    @Schema(description = "访问令牌", requiredMode = Schema.RequiredMode.REQUIRED) String token,
    @Schema(description = "刷新令牌", requiredMode = Schema.RequiredMode.REQUIRED)
        String refreshToken) {}
