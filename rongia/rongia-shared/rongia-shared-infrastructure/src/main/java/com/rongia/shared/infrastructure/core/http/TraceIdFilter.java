package com.rongia.shared.infrastructure.core.http;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** 请求ID过滤器：为每个请求分配唯一ID，并放入MDC和响应头。 优先使用调用方传入的 X-Trace-Id，否则生成新的 UUID。 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // 确保在所有其他过滤器之前执行
public class TraceIdFilter extends OncePerRequestFilter {

  /** 请求头名称：调用方可通过此头传入 traceId */
  private static final String TRACE_ID_HEADER = "X-Trace-Id";

  /** MDC 中存放 traceId 的 key */
  private static final String MDC_KEY = "traceId";

  /**
   * 为每个请求分配 traceId，写入 MDC 和响应头
   *
   * @param request 请求
   * @param response 响应
   * @param filterChain 过滤器链
   * @throws ServletException Servlet 异常
   * @throws IOException IO 异常
   */
  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    // 1. 获取或生成 Request ID
    String traceId = request.getHeader(TRACE_ID_HEADER);
    if (traceId == null || traceId.isBlank()) {
      traceId = UUID.randomUUID().toString().toUpperCase();
    }
    // 放入 request 属性，供 HttpContextHolder 等处读取
    request.setAttribute(MDC_KEY, traceId);
    // 2. 存入 MDC，供日志使用
    MDC.put(MDC_KEY, traceId);
    // 3. 放入响应头，方便前端或下游排查
    response.setHeader(TRACE_ID_HEADER, traceId);

    try {
      filterChain.doFilter(request, response);
    } finally {
      // 4. 清理 MDC，防止线程池复用污染
      MDC.remove(MDC_KEY);
    }
  }
}
