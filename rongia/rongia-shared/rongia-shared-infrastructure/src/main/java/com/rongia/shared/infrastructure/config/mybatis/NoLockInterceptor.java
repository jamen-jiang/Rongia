package com.rongia.shared.infrastructure.config.mybatis;

import java.sql.Connection;
import java.util.Properties;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

/**
 * 给所有 SELECT 语句自动追加 WITH(NOLOCK)。
 *
 * <p>SQL Server 的表提示语法是「表名 [别名] WITH(NOLOCK)」，提示必须放在别名之后， 否则会报「"别名"附近有语法错误」。
 *
 * <p>只处理以 SELECT 开头的 SQL；已经含 WITH(NOLOCK) 的不重复追加。
 */
@Intercepts({
  @Signature(
      type = StatementHandler.class,
      method = "prepare",
      args = {Connection.class, Integer.class})
})
public class NoLockInterceptor implements Interceptor {

  /**
   * 拦截 StatementHandler.prepare，改写 boundSql。
   *
   * @param invocation 调用信息
   * @return 原方法返回值
   * @throws Throwable 原方法抛出的异常
   */
  @Override
  public Object intercept(Invocation invocation) throws Throwable {
    // 拿到 StatementHandler
    StatementHandler handler = (StatementHandler) invocation.getTarget();
    MetaObject mo = SystemMetaObject.forObject(handler);

    // 取出待执行的 SQL
    String sql = (String) mo.getValue("delegate.boundSql.sql");
    if (sql == null
        || sql.toUpperCase().contains("WITH(NOLOCK)") // 已加过，跳过
        || !sql.trim().toUpperCase().startsWith("SELECT")) { // 非 SELECT，跳过
      return invocation.proceed();
    }

    // 把 FROM 子句改成：FROM 表名 [别名] WITH(NOLOCK)
    // 把 JOIN 子句改成：JOIN 表名 [别名] WITH(NOLOCK)
    // 别名可选；负向断言排除 WHERE / ON / JOIN 等关键字被误当成别名
    String newSql =
        sql.replaceAll(
                // 匹配 FROM + 表名 + 可选别名（别名不能是 SQL 关键字）
                "(?i)\\bFROM\\s+([\\w\\.\\[\\]]+)(\\s+(?!(?i:WHERE|ON|JOIN|INNER|LEFT|RIGHT|FULL|CROSS|OUTER|GROUP|ORDER|HAVING|WITH|UNION|,))\\w+)?",
                "FROM $1$2 WITH(NOLOCK)")
            .replaceAll(
                // 匹配 JOIN + 表名 + 可选别名（别名不能是 SQL 关键字）
                "(?i)\\bJOIN\\s+([\\w\\.\\[\\]]+)(\\s+(?!(?i:ON|WHERE|JOIN|INNER|LEFT|RIGHT|FULL|CROSS|OUTER|GROUP|ORDER|HAVING|WITH|UNION|,))\\w+)?",
                "JOIN $1$2 WITH(NOLOCK)");

    // 写回修改后的 SQL
    mo.setValue("delegate.boundSql.sql", newSql);

    return invocation.proceed();
  }

  /**
   * 创建代理对象。
   *
   * @param target 被拦截的目标对象
   * @return 代理对象
   */
  @Override
  public Object plugin(Object target) {
    return Interceptor.super.plugin(target);
  }

  /**
   * 设置拦截器属性（本拦截器无需额外属性）。
   *
   * @param properties 属性
   */
  @Override
  public void setProperties(Properties properties) {
    Interceptor.super.setProperties(properties);
  }
}
