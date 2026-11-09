package com.rongia.shared.infrastructure.core.lock;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.jspecify.annotations.Nullable;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/** 锁 key / 条件解析器（SpEL） */
public final class LockKeyResolver {

  /** SpEL 解析器 */
  private static final ExpressionParser PARSER = new SpelExpressionParser();

  /** 参数名发现器 */
  private static final ParameterNameDiscoverer NAME_DISCOVERER =
      new DefaultParameterNameDiscoverer();

  /** 表达式缓存 */
  private static final Map<String, Expression> EXPRESSION_CACHE = new ConcurrentHashMap<>();

  private LockKeyResolver() {}

  /**
   * 解析锁 key
   *
   * @param pjp 切点
   * @param expression SpEL 表达式
   * @return 锁 key
   */
  public static String resolve(ProceedingJoinPoint pjp, @Nullable String expression) {
    MethodSignature signature = (MethodSignature) pjp.getSignature();
    Method method = signature.getMethod();
    if (expression == null || expression.isBlank()) {
      return signature.getDeclaringType().getSimpleName() + "_" + method.getName();
    }
    EvaluationContext context =
        new MethodBasedEvaluationContext(pjp.getTarget(), method, pjp.getArgs(), NAME_DISCOVERER);
    Object value = parse(expression).getValue(context);
    return value == null ? "null" : value.toString();
  }

  /**
   * 求值条件表达式
   *
   * @param pjp 切点
   * @param condition SpEL 条件
   * @return true 表示满足条件
   */
  public static boolean evaluateCondition(ProceedingJoinPoint pjp, @Nullable String condition) {
    if (condition == null || condition.isBlank()) {
      return true;
    }
    Method method = ((MethodSignature) pjp.getSignature()).getMethod();
    EvaluationContext context =
        new MethodBasedEvaluationContext(pjp.getTarget(), method, pjp.getArgs(), NAME_DISCOVERER);
    Boolean result = parse(condition).getValue(context, Boolean.class);
    return Boolean.TRUE.equals(result);
  }

  /** 解析表达式（带缓存） */
  private static Expression parse(String expression) {
    return EXPRESSION_CACHE.computeIfAbsent(expression, PARSER::parseExpression);
  }
}
