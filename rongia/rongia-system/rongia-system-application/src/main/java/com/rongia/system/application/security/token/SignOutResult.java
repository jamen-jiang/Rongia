package com.rongia.system.application.security.token;

/**
 * 登出结果（携带用户标识，供应用层记日志用）
 *
 * @param userId 用户Id
 * @param userName 账号
 * @param name 用户名称
 */
public record SignOutResult(Long userId, String userName, String name) {}
