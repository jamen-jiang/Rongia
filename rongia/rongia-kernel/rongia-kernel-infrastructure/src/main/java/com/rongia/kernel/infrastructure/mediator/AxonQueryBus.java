package com.rongia.kernel.infrastructure.mediator;

import com.rongia.kernel.application.mediator.Query;
import com.rongia.kernel.application.mediator.QueryBus;
import java.util.List;
import org.axonframework.messaging.queryhandling.gateway.QueryGateway;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * 查询总线实现（基于 Axon 的 {@link QueryGateway}）
 *
 * <p>纯委托：Axon 的 gateway 返回 {@code CompletableFuture}，本类负责 {@code join()} 把它拉回同步语义； 不加工查询、不改结果、不吞异常，异常原样往上抛。
 */
@NullMarked
@Component
public class AxonQueryBus implements QueryBus {

  /** Axon 查询网关 */
  private final QueryGateway queryGateway;

  /**
   * 构造器注入
   *
   * @param queryGateway Axon 查询网关
   */
  public AxonQueryBus(QueryGateway queryGateway) {
    this.queryGateway = queryGateway;
  }

  /**
   * 查询单条结果
   *
   * @param query 查询条件
   * @param resultType 期望的结果类型
   * @param <R> 结果类型
   * @return 查询结果；查询处理器返回 null 时此处也为 null
   */
  @Override
  public <R> @Nullable R query(Query query, Class<R> resultType) {
    return queryGateway.query(query, resultType).join();
  }

  /**
   * 查询多条结果
   *
   * @param query 查询条件
   * @param resultType 期望的结果类型
   * @param <R> 结果类型
   * @return 查询结果列表，无结果时为空列表
   */
  @Override
  public <R> List<R> queryMany(Query query, Class<R> resultType) {
    return queryGateway.queryMany(query, resultType).join();
  }
}
