package com.rongia.production.domain.jobcard;

import com.rongia.production.domain.workorder.WorkOrder;
import com.rongia.production.domain.workorder.WorkOrderOperation;
import com.rongia.production.domain.workorder.WorkOrderRepository;
import com.rongia.kernel.domain.exception.DomainException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 工卡领域服务（报工）
 *
 * <p>照 ERPNext 的作业卡节奏：报工不另建单据，就是往这道工序的工卡上加一条工时日志，卡头按有效日志汇总，
 * 再把合计回写到工单工序行。
 *
 * <p>一道工序一张卡：第一次报工时建卡，之后都往同一张卡上加。要撤某一枪就作废那条日志，卡头与工序行一起减回去。
 */
@NullMarked
public class JobCardDomainService {

  /** 工时保留位数，与时间列 decimal(18, 2) 对齐 */
  private static final int MINUTE_SCALE = 2;

  /** 工卡工厂 */
  private final JobCardFactory jobCardFactory;

  /** 工卡仓储 */
  private final JobCardRepository jobCardRepository;

  /** 工单仓储 */
  private final WorkOrderRepository workOrderRepository;

  /**
   * 构造器注入
   *
   * @param jobCardFactory 工卡工厂
   * @param jobCardRepository 工卡仓储
   * @param workOrderRepository 工单仓储
   */
  public JobCardDomainService(
      JobCardFactory jobCardFactory,
      JobCardRepository jobCardRepository,
      WorkOrderRepository workOrderRepository) {
    this.jobCardFactory = jobCardFactory;
    this.jobCardRepository = jobCardRepository;
    this.workOrderRepository = workOrderRepository;
  }

  /**
   * 报工：往工卡上加一笔，并回写工序行合计
   *
   * <p>先让工单侧校验通过再落日志——反过来的话，工单侧一抛异常就会留下一笔「报了工但没汇总」的记录。
   *
   * @param workOrderId 工单Id
   * @param workOrderOperationId 工单工序Id
   * @param completedQty 本次良品数量
   * @param lossQty 本次报废数量
   * @param workTime 本次工时（分钟），传空则按工序标准工时折算
   * @param remark 备注（可空）
   * @param moment 报工时刻
   * @return 工卡（卡上已带上这笔日志）
   * @throws DomainException 工单或工序行不存在、数量非法、或累计报工数超过可投入数量时抛出
   */
  public JobCard report(
      long workOrderId,
      long workOrderOperationId,
      BigDecimal completedQty,
      BigDecimal lossQty,
      @Nullable BigDecimal workTime,
      @Nullable String remark,
      LocalDateTime moment) {
    if (completedQty.signum() < 0 || lossQty.signum() < 0) {
      throw new DomainException("报工数量不能为负数");
    }
    if (completedQty.signum() == 0 && lossQty.signum() == 0) {
      throw new DomainException("良品数量与报废数量不能同时为0");
    }
    WorkOrder workOrder = requireWorkOrder(workOrderId);
    WorkOrderOperation operation = requireOperation(workOrder, workOrderOperationId);
    workOrder.reportOperation(operation.getId(), completedQty);
    checkReportGate(workOrder, operation, completedQty);

    JobCard card = jobCardRepository.getByWorkOrderOperationId(operation.getId());
    if (card == null || card.isCancelled()) {
      // 传空串：单号交给 OrderAddedEvent 的监听器分配
      card =
          jobCardFactory.create(
              workOrder.getId(),
              workOrder.getNumber(),
              operation.getId(),
              operation.getOperationName(),
              workOrder.getQty(),
              null,
              "");
    }
    BigDecimal minutes = resolveWorkTime(operation, completedQty, lossQty, workTime);
    card.addTimeLog(null, null, minutes, completedQty, remark, moment);
    if (lossQty.signum() > 0) {
      card.addLossQty(lossQty);
    }
    workOrder.applyOperationTotals(
        operation.getId(),
        operation.getCompletedQty().add(completedQty),
        operation.getLossQty().add(lossQty));
    return card;
  }

  /**
   * 删掉一笔报工：卡头与工序行一起减回去（仅未提交的卡）
   *
   * @param jobCardId 工卡Id
   * @param timeLogId 工时日志Id
   * @param moment 操作时刻
   * @return 工卡
   * @throws DomainException 工卡或日志不存在、卡已提交/已取消、后续工序已报工、或工单侧校验不过时抛出
   */
  public JobCard deleteTimeLog(long jobCardId, long timeLogId, LocalDateTime moment) {
    JobCard card = getExist(jobCardId);
    JobCardTimeLog log =
        card.getTimeLogs().stream()
            .filter(l -> l.getId().equals(timeLogId))
            .findFirst()
            .orElseThrow(() -> new DomainException("工时日志" + timeLogId + "不存在!"));
    WorkOrder workOrder = requireWorkOrder(card.getWorkOrderId());
    workOrder.unreportOperation(card.getWorkOrderOperationId());
    card.deleteTimeLog(timeLogId);
    WorkOrderOperation operation =
        requireOperation(workOrder, card.getWorkOrderOperationId());
    workOrder.applyOperationTotals(
        operation.getId(),
        operation.getCompletedQty().subtract(log.getCompletedQty()),
        operation.getLossQty());
    return card;
  }

  /**
   * 提交工卡：卡锁死，之后不能再加删报工行
   *
   * @param jobCardId 工卡Id
   * @param moment 提交时刻
   * @return 工卡
   * @throws DomainException 工卡不存在、已提交/已取消、或一笔报工都没有时抛出
   */
  public JobCard submit(long jobCardId, LocalDateTime moment) {
    JobCard card = getExist(jobCardId);
    card.submit(moment);
    return card;
  }

  /**
   * 取消整张工卡：卡上报过的账一起退回工序行
   *
   * <p>取消不是「这道工序不做了」——工序行照旧留着，之后可以重新建一张卡接着报。
   *
   * @param jobCardId 工卡Id
   * @param moment 取消时间
   * @return 工卡
   * @throws DomainException 工卡不存在、已取消、或后续工序已报工时抛出
   */
  public JobCard cancel(long jobCardId, LocalDateTime moment) {
    JobCard card = getExist(jobCardId);
    if (card.isCancelled()) {
      throw new DomainException("工卡" + card.getNumber() + "已取消");
    }
    WorkOrder workOrder = requireWorkOrder(card.getWorkOrderId());
    WorkOrderOperation operation =
        requireOperation(workOrder, card.getWorkOrderOperationId());
    if (card.getCompletedQty().signum() != 0 || card.getLossQty().signum() != 0) {
      workOrder.unreportOperation(operation.getId());
    }
    BigDecimal completedQty = card.getCompletedQty();
    BigDecimal lossQty = card.getLossQty();
    card.cancel(moment);
    workOrder.applyOperationTotals(
        operation.getId(),
        operation.getCompletedQty().subtract(completedQty),
        operation.getLossQty().subtract(lossQty));
    return card;
  }

  /**
   * 报工数量闸：本笔生效后的流水合计不得超过本工序的可投入数量
   *
   * @param workOrder 工单聚合
   * @param operation 工单工序行（回写之前的瞬时合计）
   * @param completedQty 本次良品数量
   * @throws DomainException 累计报工数超过可投入数量时抛出
   */
  private void checkReportGate(
      WorkOrder workOrder, WorkOrderOperation operation, BigDecimal completedQty) {
    BigDecimal total = operation.getCompletedQty().add(completedQty);
    BigDecimal availableQty = workOrder.availableQtyFor(operation.getId());
    if (total.compareTo(availableQty) > 0) {
      throw new DomainException(
          "工序" + operation.getOperationName() + "累计报工数" + total + "超过可投入数量" + availableQty);
    }
  }

  /**
   * 定本次工时：现场填了就用现场的，没填就按工序标准工时折算
   *
   * <p>产出量取良品加报废——报废件同样吃掉了加工时间。
   *
   * @param operation 工单工序行
   * @param completedQty 本次良品数量
   * @param lossQty 本次报废数量
   * @param workTime 现场填报工时（可空）
   * @return 本次工时（分钟）
   */
  private BigDecimal resolveWorkTime(
      WorkOrderOperation operation,
      BigDecimal completedQty,
      BigDecimal lossQty,
      @Nullable BigDecimal workTime) {
    if (workTime != null) {
      return workTime;
    }
    BigDecimal output = completedQty.add(lossQty);
    return operation.estimatedMinutes(output).setScale(MINUTE_SCALE, RoundingMode.HALF_UP);
  }

  /**
   * 取工卡，不存在则抛异常
   *
   * @param jobCardId 工卡Id
   * @return 工卡聚合
   * @throws DomainException 工卡不存在时抛出
   */
  private JobCard getExist(long jobCardId) {
    JobCard model = jobCardRepository.get(jobCardId);
    if (model == null) {
      throw new DomainException("工卡" + jobCardId + "不存在!");
    }
    return model;
  }

  /**
   * 取工单，不存在则抛异常
   *
   * @param workOrderId 工单Id
   * @return 工单聚合
   * @throws DomainException 工单不存在时抛出
   */
  private WorkOrder requireWorkOrder(long workOrderId) {
    WorkOrder workOrder = workOrderRepository.get(workOrderId);
    if (workOrder == null) {
      throw new DomainException("工单" + workOrderId + "不存在!");
    }
    return workOrder;
  }

  /**
   * 在这张工单内取工序行
   *
   * @param workOrder 工单聚合
   * @param workOrderOperationId 工单工序Id
   * @return 工单工序行
   * @throws DomainException 工序行不属于这张工单时抛出
   */
  private WorkOrderOperation requireOperation(WorkOrder workOrder, long workOrderOperationId) {
    return workOrder.getWorkOrderOperations().stream()
        .filter(operation -> operation.getId().equals(workOrderOperationId))
        .findFirst()
        .orElseThrow(
            () ->
                new DomainException(
                    "工单" + workOrder.getNumber() + "下没有Id为" + workOrderOperationId + "的工序行"));
  }
}
