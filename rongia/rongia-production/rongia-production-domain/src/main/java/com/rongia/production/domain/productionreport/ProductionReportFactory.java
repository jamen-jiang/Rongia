package com.rongia.production.domain.productionreport;

import com.rongia.shared.domain.exception.DomainException;
import java.math.BigDecimal;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 生产报工单工厂，负责创建报工单 */
@NullMarked
public class ProductionReportFactory {

  /** 报工单仓储 */
  private final ProductionReportRepository productionReportRepository;

  /**
   * 构造器注入
   *
   * @param productionReportRepository 报工单仓储
   */
  public ProductionReportFactory(ProductionReportRepository productionReportRepository) {
    this.productionReportRepository = productionReportRepository;
  }

  /**
   * 创建报工单
   *
   * @param workOrderId 工单Id
   * @param workOrderNumber 工单号（快照）
   * @param workOrderOperationId 工单工序Id
   * @param operationId 工序Id（快照）
   * @param operationName 工序名称（快照）
   * @param workCenterId 工作中心Id（快照）
   * @param workCenterName 工作中心名称（快照）
   * @param operatorId 操作人Id（可空）
   * @param workTime 本次工时（分钟）
   * @param goodQty 本次良品数量
   * @param lossQty 本次报废数量
   * @param remark 备注（可空）
   * @param number 单号（传空串表示由 OrderAddedEvent 的监听器分配）
   * @return 报工单聚合
   * @throws DomainException 单号已存在时抛出
   */
  public ProductionReport create(
      long workOrderId,
      String workOrderNumber,
      long workOrderOperationId,
      long operationId,
      String operationName,
      long workCenterId,
      String workCenterName,
      @Nullable Long operatorId,
      BigDecimal workTime,
      BigDecimal goodQty,
      BigDecimal lossQty,
      @Nullable String remark,
      String number) {
    // 单号非空时才校验唯一，空串代表尚未分配
    if (!number.isBlank() && productionReportRepository.isExistByNumberExcludeId(number)) {
      throw new DomainException("报工单号" + number + "已存在!");
    }
    return new ProductionReport(
        workOrderId,
        workOrderNumber,
        workOrderOperationId,
        operationId,
        operationName,
        workCenterId,
        workCenterName,
        operatorId,
        workTime,
        goodQty,
        lossQty,
        remark,
        number);
  }
}
