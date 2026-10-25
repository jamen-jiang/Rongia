package com.rongia.shared.infrastructure.core.job;

import java.lang.reflect.Method;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * 任务调用器：通过反射调用目标 Bean 的方法。
 *
 * <p>JobRunr 的 lambda 需要可序列化，反射对象（Method、Bean）无法序列化， 所以 lambda 里只捕获 beanName 和 methodName
 * 两个字符串，执行时通过这个类反射调用。
 */
@NullMarked
@Component
public class JobInvoker {

  /** Spring 上下文 */
  private final ApplicationContext applicationContext;

  /**
   * 构造器注入
   *
   * @param applicationContext Spring 上下文
   */
  public JobInvoker(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  /**
   * 反射调用目标方法
   *
   * @param beanName Bean 名称
   * @param methodName 方法名
   */
  public void invoke(String beanName, String methodName) {
    try {
      Object bean = applicationContext.getBean(beanName);
      Method method = bean.getClass().getMethod(methodName);
      method.invoke(bean);
    } catch (Exception e) {
      throw new RuntimeException("任务执行失败：" + beanName + "." + methodName, e);
    }
  }
}
