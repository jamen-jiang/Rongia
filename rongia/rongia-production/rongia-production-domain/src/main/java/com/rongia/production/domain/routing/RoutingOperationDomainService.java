package com.rongia.production.domain.routing;

import com.rongia.production.domain.operation.Operation;
import com.rongia.production.domain.operation.OperationRepository;
import com.rongia.production.domain.workcenter.WorkCenter;
import com.rongia.production.domain.workcenter.WorkCenterRepository;
import com.rongia.shared.domain.exception.DomainException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 路线工序领域服务
 *
 * <p>路线工序是 {@code Routing} 的子行，增删改都由路线聚合自己维护；这里只做一件聚合做不了的事：
 * 建行时去工序字典与工作中心上取默认值（工作中心、周期时间、批量、费率）。这些是跨聚合取数，
 * 所以放在领域服务，取好值再交给路线。
 */
@NullMarked
public class RoutingOperationDomainService {

  /** 工序仓储：建工序行时取工序字典上的默认值 */
  private final OperationRepository operationRepository;

  /** 工作中心仓储：建工序行时取默认小时费率 */
  private final WorkCenterRepository workCenterRepository;

  /**
   * 构造器注入
   *
   * @param operationRepository 工序仓储
   * @param workCenterRepository 工作中心仓储
   */
  public RoutingOperationDomainService(
      OperationRepository operationRepository, WorkCenterRepository workCenterRepository) {
    this.operationRepository = operationRepository;
    this.workCenterRepository = workCenterRepository;
  }

  /**
   * 把入参补全成可装配的工序行数据
   *
   * <p>入参没给工作中心、周期时间、批量，就取工序字典上的默认值；费率没给就取工作中心的。
   *
   * @param data 原始入参
   * @return 补全后的工序行数据
   * @throws DomainException 工序不存在、工序没有默认工作中心、或工作中心不存在时抛出
   */
  public RoutingOperationData resolve(RoutingOperationData data) {
    Operation operation = operationRepository.get(data.operationId());
    if (operation == null) {
      throw new DomainException("工序" + data.operationId() + "不存在!");
    }
    long workCenterId =
        data.workCenterId() != null ? data.workCenterId() : defaultWorkCenterId(operation);
    return new RoutingOperationData(
        data.operationId(),
        workCenterId,
        data.cycleTime() != null ? data.cycleTime() : operation.getCycleTime(),
        data.batchSize() != null ? data.batchSize() : operation.getBatchSize(),
        data.hourlyRate() != null ? data.hourlyRate() : defaultHourlyRate(workCenterId),
        data.sequence());
  }

  /**
   * 按顺序同步一条路线的工序行
   *
   * <p>按顺序匹配：删除不在输入中的、更新已存在的、添加新的。入参为 {@code null} 视为清空。
   *
   * @param routing 工艺路线
   * @param operations 目标工序行列表（可空）
   * @throws DomainException 入参非法或取默认值失败时抛出
   */
  public void sync(Routing routing, @Nullable List<RoutingOperationData> operations) {
    List<RoutingOperationData> target = operations == null ? List.of() : operations;

    // 1. 删除未在输入中出现的（按顺序匹配）
    for (RoutingOperation current : routing.getRoutingOperations()) {
      boolean keep =
          target.stream().anyMatch(d -> Objects.equals(d.sequence(), current.getSequence()));
      if (!keep) {
        routing.removeOperation(current.getId());
      }
    }

    // 2. 遍历输入：存在则修改，不存在则添加
    for (RoutingOperationData data : target) {
      RoutingOperation current =
          routing.getRoutingOperations().stream()
              .filter(o -> Objects.equals(o.getSequence(), data.sequence()))
              .findFirst()
              .orElse(null);
      if (current != null) {
        // 存在：修改；入参没给的值保留原值
        current.modify(
            data.operationId(),
            data.workCenterId() != null ? data.workCenterId() : current.getWorkCenterId(),
            data.cycleTime() != null ? data.cycleTime() : current.getCycleTime(),
            data.batchSize() != null ? data.batchSize() : current.getBatchSize(),
            data.hourlyRate() != null ? data.hourlyRate() : current.getHourlyRate(),
            data.sequence());
      } else {
        RoutingOperationData resolved = resolve(data);
        routing.addOperation(
            resolved.operationId(),
            resolved.workCenterId(),
            resolved.cycleTime(),
            resolved.batchSize(),
            resolved.hourlyRate(),
            resolved.sequence());
      }
    }
  }

  /**
   * 工序字典上的默认工作中心，没配就报错
   *
   * <p>工序行的工作中心是必填的：字典和入参都没给，就说明这道工序不知道该在哪做，不能默认塞一个。
   *
   * @param operation 工序
   * @return 默认工作中心Id
   * @throws DomainException 工序没有默认工作中心时抛出
   */
  private long defaultWorkCenterId(Operation operation) {
    Long workCenterId = operation.getWorkCenterId();
    if (workCenterId == null) {
      throw new DomainException(
          "工序" + operation.getName() + "没有默认工作中心，请在工序行指定工作中心");
    }
    return workCenterId;
  }

  /**
   * 取工作中心的小时费率，作为工序行的默认费率
   *
   * @param workCenterId 工作中心Id
   * @return 小时费率
   * @throws DomainException 工作中心不存在时抛出
   */
  private BigDecimal defaultHourlyRate(long workCenterId) {
    WorkCenter workCenter = workCenterRepository.get(workCenterId);
    if (workCenter == null) {
      throw new DomainException("工作中心" + workCenterId + "不存在!");
    }
    return workCenter.getHourlyRate();
  }
}
