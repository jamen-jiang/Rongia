package com.rongia.shared.infrastructure.config;

import com.rongia.shared.infrastructure.websocket.AuthChannelInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket + STOMP 配置。
 *
 * <p>对应前端：
 *
 * <ul>
 *   <li>端点 /ws —— 前端 new SockJS('/ws') 连接
 *   <li>用户前缀 /user —— 前端订阅 '/user/queue/xxx'，服务端 convertAndSendToUser 不带 /user
 * </ul>
 *
 * <p>本项目只做服务端 → 客户端推送，不需要 @MessageMapping 接收。
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final AuthChannelInterceptor authChannelInterceptor;

  public WebSocketConfig(AuthChannelInterceptor authChannelInterceptor) {
    this.authChannelInterceptor = authChannelInterceptor;
  }

  /**
   * 注册 STOMP 端点。
   *
   * <p>withSockJS() 的作用：浏览器或代理不支持原生 WebSocket 时， 自动降级为 HTTP 长轮询等方案。前端需用 new SockJS('/ws') 连接。
   */
  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry
        .addEndpoint("/ws")
        .setAllowedOriginPatterns("*") // ★ 用这个代替 setAllowedOrigins("*")
        .withSockJS();
  }

  /**
   * 配置消息代理。
   *
   * <ul>
   *   <li>/queue —— 点对点目的地，一对一推送
   *   <li>/topic —— 广播目的地，一对多推送（暂时用不到可保留）
   *   <li>/user —— 用户专属前缀，配合 convertAndSendToUser 使用
   * </ul>
   */
  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableSimpleBroker("/topic", "/queue");
    registry.setUserDestinationPrefix("/user");
  }

  /**
   * 注册入站认证拦截器。
   *
   * <p>拦截器在 CONNECT 帧时校验 JWT，认证一次后 Spring 自动绑定 Principal 到会话。后续所有推送都按 Principal 定位用户。
   */
  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(authChannelInterceptor);
  }
}
