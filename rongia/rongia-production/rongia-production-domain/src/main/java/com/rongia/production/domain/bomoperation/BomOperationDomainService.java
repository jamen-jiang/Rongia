package com.rongia.production.domain.bomoperation;

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

/** 工序行领域服务，负责按父级同步工序行 */
@NullMarked
public class BomOperationDomainService {

  /** 工序行工厂 */
  private final BomOperationFactory bomOperationFactory;

  /** 工序行仓储 */
  private final BomOperationRepository bomOperationRepository;

  /** 工序仓储：建工序行时取工序字典上的默认值 */
  private final OperationRepository operationRepository;

  /** 工作中心仓储：建工序行时取默认小时费率 */
  private final WorkCenterRepository workCenterRepository;

  /**
   * 构造器注入
   *
   * @param bomOperationFactory 工序行工厂
   * @param bomOperationRepository 工序行仓储
   * @param operationRepository 工序仓储
   * @param workCenterRepository 工作中心仓储
   */
  public BomOperationDomainService(
      BomOperationFactory bomOperationFactory,
      BomOperationRepository bomOperationRepository,
      OperationRepository operationRepository,
      WorkCenterRepository workCenterRepository) {
    this.bomOperationFactory = bomOperationFactory;
    this.bomOperationRepository = bomOperationRepository;
    this.operationRepository = operationRepository;
    this.workCenterRepository = workCenterRepository;
  }

  /**
   * 按父级同步工序行（按顺序匹配：删除不在输入中的、更新已存在的、添加新的）
   *
   * @param parentType 父级类型编码
   * @param parentId 父级Id
   * @param operations 目标工序行列表（可空，null 视为清空）
   */
  public void sync(
      String parentType,
      long parentId,
      @Nullable List<BomOperationData> operations) {
    List<BomOperationData> target = operations == null ? List.of() : operations;
    List<BomOperation> existing = bomOperationRepository.listByParent(parentType, parentId);

    // 1. 删除未在输入中出现的（按顺序匹配）
    for (BomOperation current : existing) {
      boolean keep =
          target.stream().anyMatch(d -> Objects.equals(d.sequence(), current.getSequence()));
      if (!keep) {
        current.delete();
        bomOperationRepository.save(current);
      }
    }

    // 2. 遍历输入：存在则修改，不存在则添加
    for (BomOperationData data : target) {
      BomOperation current =
          existing.stream()
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
        bomOperationRepository.save(current);
      } else {
        // 不存在：添加；未给的值取工序字典上的默认值，费率取工作中心的
        Operation operation = operationRepository.get(data.operationId());
        if (operation == null) {
          throw new DomainException("工序" + data.operationId() + "不存在!");
        }
        long workCenterId =
            data.workCenterId() != null ? data.workCenterId() : defaultWorkCenterId(operation);
        bomOperationRepository.save(
            bomOperationFactory.create(
                parentType,
                parentId,
                data.operationId(),
                workCenterId,
                data.cycleTime() != null ? data.cycleTime() : operation.getCycleTime(),
                data.batchSize() != null ? data.batchSize() : operation.getBatchSize(),
                data.hourlyRate() != null
                    ? data.hourlyRate()
                    : defaultHourlyRate(workCenterId),
                data.sequence()));
      }
    }
  }

  /**
   * 取某条工序行，不存在则抛异常
   *
   * @param id 工序行Id
   * @return 工序行聚合
   * @throws DomainException 不存在时抛出
   */
  public BomOperation getExist(long id) {
    BomOperation model = bomOperationRepository.get(id);
    if (model == null) {
      throw new DomainException("工序行" + id + "不存在!");
    }
    return model;
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

  /**
   * 修改单条工序行
   *
   * @param id 工序行Id
   * @param operationId 工序Id
   * @param workCenterId 工作中心Id
   * @param cycleTime 周期时间（分钟）
   * @param batchSize 批量大小（件）
   * @param hourlyRate 小时费率（元/小时）
   * @param sequence 工序顺序
   * @return 工序行聚合
   */
  public BomOperation modify(
      long id,
      long operationId,
      long workCenterId,
      BigDecimal cycleTime,
      BigDecimal batchSize,
      BigDecimal hourlyRate,
      int sequence) {
    BomOperation model = getExist(id);
    if (bomOperationRepository.isExistSequenceExcludeId(
        model.getParentType(), model.getParentId(), sequence, id)) {
      throw new DomainException("工序顺序" + sequence + "已存在!");
    }
    model.modify(operationId, workCenterId, cycleTime, batchSize, hourlyRate, sequence);
    return model;
  }
}
