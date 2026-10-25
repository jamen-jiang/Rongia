package com.rongia.shared.infrastructure.config.jpa;

import com.rongia.shared.application.security.user.CurrentUser;
import com.rongia.shared.domain.entity.CreateEntity;
import com.rongia.shared.domain.entity.UpdateEntity;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/** Hibernate 审计事件监听：在 insert/update 前填充审计字段，domain 层不感知 */
@Component
public class AuditEventListener implements PreInsertEventListener, PreUpdateEventListener {

  /** 当前登录用户 */
  private final CurrentUser currentUser;

  /**
   * 构造器注入
   *
   * @param currentUser 当前登录用户
   */
  public AuditEventListener(CurrentUser currentUser) {
    this.currentUser = currentUser;
  }

  /**
   * 插入前填充创建审计字段
   *
   * @param event 插入事件
   * @return false 表示不拦截，继续执行
   */
  @Override
  public boolean onPreInsert(PreInsertEvent event) {
    Object entity = event.getEntity();
    if (entity instanceof CreateEntity) {
      Object[] state = event.getState();
      String[] propertyNames = event.getPersister().getPropertyNames();
      setValue(entity, state, propertyNames, "createdBy", currentUser.getUserId());
      setValue(entity, state, propertyNames, "createdByName", currentUser.getName());
      setValue(entity, state, propertyNames, "createdTime", LocalDateTime.now());
    }
    return false;
  }

  /**
   * 更新前填充更新审计字段
   *
   * @param event 更新事件
   * @return false 表示不拦截，继续执行
   */
  @Override
  public boolean onPreUpdate(PreUpdateEvent event) {
    Object entity = event.getEntity();
    if (entity instanceof UpdateEntity) {
      Object[] state = event.getState();
      String[] propertyNames = event.getPersister().getPropertyNames();
      setValue(entity, state, propertyNames, "updatedBy", currentUser.getUserId());
      setValue(entity, state, propertyNames, "updatedByName", currentUser.getName());
      setValue(entity, state, propertyNames, "updatedTime", LocalDateTime.now());
    }
    return false;
  }

  /**
   * 同时写 entity 字段和 Hibernate state 数组，保证 SQL 里带的是新值
   *
   * @param entity 实体
   * @param state Hibernate 状态数组
   * @param propertyNames 属性名数组
   * @param property 要设置的属性名
   * @param value 要设置的值
   */
  private void setValue(
      Object entity, Object[] state, String[] propertyNames, String property, Object value) {
    for (int i = 0; i < propertyNames.length; i++) {
      if (property.equals(propertyNames[i])) {
        state[i] = value;
        break;
      }
    }
    try {
      Field field = findField(entity.getClass(), property);
      if (field != null) {
        field.setAccessible(true);
        field.set(entity, value);
      }
    } catch (Exception e) {
      // 忽略，不影响主流程
    }
  }

  /**
   * 沿继承链查找字段
   *
   * @param clazz 类
   * @param name 字段名
   * @return 字段，找不到返回 null
   */
  @Nullable
  private Field findField(Class<?> clazz, String name) {
    Class<?> current = clazz;
    while (current != null && current != Object.class) {
      try {
        return current.getDeclaredField(name);
      } catch (NoSuchFieldException e) {
        current = current.getSuperclass();
      }
    }
    return null;
  }
}
