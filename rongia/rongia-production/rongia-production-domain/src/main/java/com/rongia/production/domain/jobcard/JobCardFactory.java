package com.rongia.production.domain.jobcard;

import com.rongia.shared.domain.exception.DomainException;
import java.math.BigDecimal;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 工卡工厂，负责创建工卡 */
@NullMarked
public class JobCardFactory {

  /** 工卡仓储 */
  private final JobCardRepository jobCardRepository;

  /**
   * 构造器注入
   *
   * @param jobCardRepository 工卡仓储
   */
  public JobCardFactory(JobCardRepository jobCardRepository) {
    this.jobCardRepository = jobCardRepository;
  }

  /**
   * 创建工卡
   *
   * @param workOrderId 工单Id
   * @param workOrderNumber 工单号（快照）
   * @param workOrderOperationId 工单工序Id
   * @param operationName 工序名称（快照）
   * @param plannedQty 计划数量
   * @param remark 备注（可空）
   * @param number 工卡单号（传空串表示由 OrderAddedEvent 的监听器分配）
   * @return 工卡聚合
   * @throws DomainException 单号已存在时抛出
   */
  public JobCard create(
      long workOrderId,
      String workOrderNumber,
      long workOrderOperationId,
      String operationName,
      BigDecimal plannedQty,
      @Nullable String remark,
      String number) {
    if (!number.isBlank() && jobCardRepository.isExistByNumberExcludeId(number)) {
      throw new DomainException("工卡号" + number + "已存在!");
    }
    return new JobCard(
        workOrderId,
        workOrderNumber,
        workOrderOperationId,
        operationName,
        plannedQty,
        remark,
        number);
  }
}
