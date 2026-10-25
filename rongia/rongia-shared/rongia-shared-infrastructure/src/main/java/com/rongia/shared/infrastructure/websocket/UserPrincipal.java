package com.rongia.shared.infrastructure.websocket;

import java.security.Principal;

/**
 * WebSocket 会话主体。
 *
 * <p>实现 Principal，绑定到 STOMP 会话上。 之所以不用 UsernamePasswordAuthenticationToken(userId, ...) 直接存 userId，
 * 是因为 getName() 只能返回一个字段，而推送时需要 userName。
 *
 * <p>注意：getName() 返回的值必须与 convertAndSendToUser 的第一个参数一致， 否则消息发出去没人收到。
 *
 * @param userId 用户Id
 * @param userName 账号
 */
public record UserPrincipal(long userId, String userName) implements Principal {

  @Override
  public String getName() {
    // 返回账号，对应前端 authStore.userName
    return userName;
  }
}
