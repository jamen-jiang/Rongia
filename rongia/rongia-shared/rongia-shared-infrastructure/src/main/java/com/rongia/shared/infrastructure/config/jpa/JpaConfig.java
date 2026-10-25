package com.rongia.shared.infrastructure.config.jpa;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.SessionFactoryImpl;
import org.springframework.context.annotation.Configuration;

/** JPA 配置：注册 Hibernate 事件监听器 */
@Configuration
public class JpaConfig {

  /** EntityManagerFactory */
  private final EntityManagerFactory entityManagerFactory;

  /** 审计事件监听器 */
  private final AuditEventListener auditEventListener;

  /**
   * 构造器注入
   *
   * @param entityManagerFactory EntityManagerFactory
   * @param auditEventListener 审计事件监听器
   */
  public JpaConfig(
      EntityManagerFactory entityManagerFactory, AuditEventListener auditEventListener) {
    this.entityManagerFactory = entityManagerFactory;
    this.auditEventListener = auditEventListener;
  }

  /** 把监听器注册到 Hibernate SessionFactory */
  @PostConstruct
  public void registerListeners() {
    SessionFactoryImpl sessionFactory = entityManagerFactory.unwrap(SessionFactoryImpl.class);
    EventListenerRegistry registry =
        sessionFactory.getServiceRegistry().getService(EventListenerRegistry.class);
    if (registry != null) {
      // 审计字段填充
      registry.appendListeners(EventType.PRE_INSERT, auditEventListener);
      registry.appendListeners(EventType.PRE_UPDATE, auditEventListener);
    }
  }
}
