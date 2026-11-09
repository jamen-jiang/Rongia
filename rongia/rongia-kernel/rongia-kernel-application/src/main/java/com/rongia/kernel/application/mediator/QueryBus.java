package com.rongia.kernel.application.mediator;

import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 查询总线端口
 *
 * <p>应用层与适配层查询的统一入口，具体实现由基础设施层提供（如 Axon 的 QueryGateway）。调用方只依赖本接口，不感知底层用的是哪个消息框架。
 *
 * <p>本端口只有同步语义：需要异步查询时由调用方自行把结果丢到线程池，本端口不暴露 {@code CompletableFuture}。
 */
@NullMarked
public interface QueryBus {

  /**
   * 查询单条结果
   *
   * @param query 查询条件
   * @param resultType 期望的结果类型
   * @param <R> 结果类型
   * @return 查询结果；查询处理器返回 null 时此处也为 null
   */
  <R> @Nullable R query(Query query, Class<R> resultType);

  /**
   * 查询多条结果
   *
   * @param query 查询条件
   * @param resultType 期望的结果类型
   * @param <R> 结果类型
   * @return 查询结果列表，无结果时为空列表
   */
  <R> List<R> queryMany(Query query, Class<R> resultType);
}
