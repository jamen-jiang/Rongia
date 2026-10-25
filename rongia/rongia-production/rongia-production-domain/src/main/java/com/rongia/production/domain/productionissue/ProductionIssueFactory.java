package com.rongia.production.domain.productionissue;

import com.rongia.shared.domain.exception.DomainException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 生产领料单工厂，负责创建生产领料单 */
@NullMarked
public class ProductionIssueFactory {

  /** 生产领料单仓储 */
  private final ProductionIssueRepository productionIssueRepository;

  /**
   * 构造器注入
   *
   * @param productionIssueRepository 生产领料单仓储
   */
  public ProductionIssueFactory(ProductionIssueRepository productionIssueRepository) {
    this.productionIssueRepository = productionIssueRepository;
  }

  /**
   * 创建生产领料单
   *
   * @param workOrderId 工单Id
   * @param workOrderNumber 工单号（冗余，便于展示）
   * @param operatorId 经办人Id（可空）
   * @param remark 备注（可空）
   * @param number 生产领料单号（传空串表示由 OrderAddedEvent 的监听器分配）
   * @return 生产领料单聚合
   * @throws DomainException 单号已存在时抛出
   */
  public ProductionIssue create(
      long workOrderId,
      String workOrderNumber,
      @Nullable Long operatorId,
      @Nullable String remark,
      String number) {
    // 单号非空时才校验唯一，空串代表尚未分配
    if (!number.isBlank() && productionIssueRepository.isExistByNumberExcludeId(number)) {
      throw new DomainException("生产领料单号" + number + "已存在!");
    }
    return new ProductionIssue(workOrderId, workOrderNumber, operatorId, remark, number);
  }
}
