package com.rongia.shared.infrastructure.config.mybatis;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.type.JdbcType;
import org.springframework.stereotype.Component;

@Component
@Intercepts(
    @Signature(
        type = ParameterHandler.class,
        method = "setParameters",
        args = {PreparedStatement.class}))
public class LocalDateTimeJdbcTypeFixInterceptor implements Interceptor {

  @Override
  public Object intercept(Invocation invocation) throws Throwable {
    Object target = unwrapProxy(invocation.getTarget());
    MetaObject handlerMeta = SystemMetaObject.forObject(target);

    MappedStatement ms = (MappedStatement) handlerMeta.getValue("mappedStatement");
    BoundSql boundSql = (BoundSql) handlerMeta.getValue("boundSql");
    Object parameterObject = boundSql.getParameterObject();

    if (parameterObject == null) {
      return invocation.proceed();
    }

    List<ParameterMapping> mappings = boundSql.getParameterMappings();
    if (mappings == null || mappings.isEmpty()) {
      return invocation.proceed();
    }

    MetaObject paramMeta = ms.getConfiguration().newMetaObject(parameterObject);

    for (ParameterMapping pm : mappings) {
      if (pm.getJavaType() != LocalDateTime.class) {
        continue;
      }
      if (pm.getJdbcType() != null) {
        continue;
      }

      Object actualValue;
      try {
        actualValue = paramMeta.getValue(pm.getProperty());
      } catch (Exception e) {
        continue;
      }

      if (actualValue == null || actualValue instanceof LocalDateTime) {
        MetaObject pmMeta = SystemMetaObject.forObject(pm);
        pmMeta.setValue("jdbcType", JdbcType.TIMESTAMP);
      }
    }

    return invocation.proceed();
  }

  /**
   * 递归拆解 MyBatis 参数对象上的 JDK 动态代理，拿到真实目标对象
   *
   * <p>MyBatis 传给拦截器的参数常常是 Mapper 接口的代理，直接读字段读不到值；这里通过
   * {@code InvocationHandler.target} 逐层解包。解包失败时原样返回，保证拦截器不影响正常调用。
   *
   * @param target 原始参数对象
   * @return 解包后的目标对象；无法解包时返回入参本身
   */
  private Object unwrapProxy(Object target) {
    if (Proxy.isProxyClass(target.getClass())) {
      Object handler = Proxy.getInvocationHandler(target);
      try {
        Field field = handler.getClass().getDeclaredField("target");
        field.setAccessible(true);
        return unwrapProxy(field.get(handler));
      } catch (Exception e) {
        return target;
      }
    }
    return target;
  }
}
