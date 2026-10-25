package com.rongia.shared.infrastructure.config.axon;

import jakarta.annotation.PreDestroy;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.common.configuration.AxonConfiguration;
import org.axonframework.common.configuration.ConfigurationEnhancer;
import org.axonframework.extension.spring.messaging.unitofwork.SpringTransactionManager;
import org.axonframework.messaging.commandhandling.CommandBus;
import org.axonframework.messaging.commandhandling.SimpleCommandBus;
import org.axonframework.messaging.core.ApplicationContext;
import org.axonframework.messaging.core.EmptyApplicationContext;
import org.axonframework.messaging.core.Message;
import org.axonframework.messaging.core.MessageHandlerInterceptor;
import org.axonframework.messaging.core.MessageTypeResolver;
import org.axonframework.messaging.core.annotation.AnnotationMessageTypeResolver;
import org.axonframework.messaging.core.configuration.MessagingConfigurer;
import org.axonframework.messaging.core.unitofwork.SimpleUnitOfWorkFactory;
import org.axonframework.messaging.core.unitofwork.UnitOfWorkFactory;
import org.axonframework.messaging.eventhandling.EventSink;
import org.axonframework.messaging.eventhandling.SimpleEventBus;
import org.axonframework.messaging.queryhandling.QueryBus;
import org.axonframework.messaging.queryhandling.SimpleQueryBus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/** Axon 配置 手动装配 CommandBus / QueryBus / EventSink，不使用 Axon Server */
@Slf4j
@Configuration
public class AxonConfig {

  /** Messaging 配置器，用于注册各组件和拦截器 */
  private final MessagingConfigurer messagingConfigurer = MessagingConfigurer.create();

  /**
   * 命令处理器拦截器列表
   *
   * <p>由各上下文以 {@code @Component} 提供（如 {@code DistributedLockInterceptor} 在 shared-infrastructure、
   * {@code CommandLoggingInterceptor} 在 system-infrastructure），Spring 自动收齐后在此统一注册。
   *
   * <p>注意：这里注入 Axon 泛型接口而非具体类——具体拦截器类可能位于 system 等下游上下文， shared 层按依赖方向不可反向引用。
   */
  private final List<MessageHandlerInterceptor<Message>> interceptors;

  /** Axon 配置对象，用于关闭时优雅停机 */
  private AxonConfiguration axonConfiguration;

  /**
   * 构造器
   *
   * @param interceptors 命令处理器拦截器列表
   */
  public AxonConfig(List<MessageHandlerInterceptor<Message>> interceptors) {
    this.interceptors = interceptors;
  }

  /**
   * 构建并启动 Axon 配置 所有注册 + build 都放在这一个 @Bean 方法里，由 Spring 控制调用时机
   *
   * @return Axon 配置对象
   */
  @Bean
  public AxonConfiguration axonConfiguration() {
    // 手动注册 Axon 各核心组件（不使用自动配置）
    messagingConfigurer.componentRegistry(
        registry -> {
          // 应用上下文：空实现，不依赖 Spring ApplicationContext
          registry.registerComponent(
              ApplicationContext.class, config -> EmptyApplicationContext.INSTANCE);

          // 工作单元工厂：管理命令/查询处理的事务边界
          registry.registerComponent(
              UnitOfWorkFactory.class,
              config -> new SimpleUnitOfWorkFactory(config.getComponent(ApplicationContext.class)));

          // 命令总线：进程内简单实现
          registry.registerComponent(
              CommandBus.class,
              config -> new SimpleCommandBus(config.getComponent(UnitOfWorkFactory.class)));

          // 查询总线：进程内简单实现
          registry.registerComponent(
              QueryBus.class,
              config -> new SimpleQueryBus(config.getComponent(UnitOfWorkFactory.class)));

          // 事件总线：只用进程内 SimpleEventBus，不做事件溯源 / 追踪
          // 如果将来要用 Axon Server 或 JDBC Event Store，这里要换成对应实现
          registry.registerComponent(EventSink.class, config -> new SimpleEventBus());

          // 消息类型解析器：从注解推断消息类型
          registry.registerComponent(
              MessageTypeResolver.class, config -> new AnnotationMessageTypeResolver());
        });

    // 注册全局命令拦截器（对所有命令生效）
    // 由 Spring 收齐的各上下文拦截器统一在此注册，顺序为 bean 收集顺序
    log.info(">>> [Axon] 通过 MessagingConfigurer 注册 {} 个全局命令拦截器", interceptors.size());
    for (MessageHandlerInterceptor<Message> interceptor : interceptors) {
      messagingConfigurer.registerCommandHandlerInterceptor(config -> interceptor);
    }

    // 构建并启动 Axon
    this.axonConfiguration = messagingConfigurer.build();
    this.axonConfiguration.start();
    log.info(">>> [Axon] AxonConfiguration 已启动");
    return this.axonConfiguration;
  }

  /**
   * 事务配置增强器：把 Spring 的事务管理器接入 Axon
   *
   * @param txManager Spring 事务管理器
   * @return 配置增强器
   */
  @Bean
  public ConfigurationEnhancer transactionConfigurationEnhancer(
      PlatformTransactionManager txManager) {
    return registry ->
        registry.registerComponent(
            org.axonframework.messaging.core.unitofwork.transaction.TransactionManager.class,
            config -> new SpringTransactionManager(txManager));
  }

  /** 应用关闭时，优雅停止 Axon */
  @PreDestroy
  public void stop() {
    if (axonConfiguration != null) {
      axonConfiguration.shutdown();
      log.info(">>> [Axon] AxonConfiguration 已关闭");
    }
  }
}
