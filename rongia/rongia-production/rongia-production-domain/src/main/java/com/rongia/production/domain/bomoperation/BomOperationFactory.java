package com.rongia.production.domain.bomoperation;

import com.rongia.shared.domain.exception.DomainException;
import java.math.BigDecimal;
import org.jspecify.annotations.NullMarked;

/** 工序行工厂，负责创建工序行 */
@NullMarked
public class BomOperationFactory {

  /** 工序行仓储 */
  private final BomOperationRepository bomOperationRepository;

  /**
   * 构造器注入
   *
   * @param bomOperationRepository 工序行仓储
   */
  public BomOperationFactory(BomOperationRepository bomOperationRepository) {
    this.bomOperationRepository = bomOperationRepository;
  }

  /**
   * 创建工序行
   *
   * @param parentType 父级类型编码
   * @param parentId 父级Id
   * @param operationId 工序Id
   * @param workCenterId 工作中心Id
   * @param cycleTime 周期时间（分钟）
   * @param batchSize 批量大小（件）
   * @param hourlyRate 小时费率（元/小时）
   * @param sequence 工序顺序
   * @return 工序行聚合
   * @throws DomainException 同一父级下工序顺序重复时抛出
   */
  public BomOperation create(
      String parentType,
      long parentId,
      long operationId,
      long workCenterId,
      BigDecimal cycleTime,
      BigDecimal batchSize,
      BigDecimal hourlyRate,
      int sequence) {
    if (bomOperationRepository.isExistSequenceExcludeId(parentType, parentId, sequence, null)) {
      throw new DomainException("工序顺序" + sequence + "已存在!");
    }
    return new BomOperation(
        parentType,
        parentId,
        operationId,
        workCenterId,
        cycleTime,
        batchSize,
        hourlyRate,
        sequence);
  }
}
