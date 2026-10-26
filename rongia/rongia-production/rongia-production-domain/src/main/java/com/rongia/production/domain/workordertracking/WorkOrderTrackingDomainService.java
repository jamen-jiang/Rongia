package com.rongia.production.domain.workordertracking;

import com.rongia.production.domain.workorder.WorkOrder;
import com.rongia.production.domain.workorder.WorkOrderOperation;
import com.rongia.production.domain.workorder.WorkOrderRepository;
import com.rongia.shared.domain.exception.DomainException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 工单跟踪（报工单）领域服务
 *
 * <p>报工要同时动两个聚合：报工单落账，工单工序累加良品/报废/工时。这是跨聚合的一致性动作，所以放在
 * 领域服务里编排，而不是让报工单去反向修改工单。
 *
 * <p>报工没有草稿：建单那一刻就累加进工单工序。现场扫完工单号落下三个数，校验与累计同时发生，错了当场
 * 报错，而不是留到下班对账才发现。要改数就作废这一张、重新报一张，作废会把累计值减回去。
 *
 * <p>创建报工单只需要三个业务事实：哪道工序、出了多少良品、坏了多少。工单号、工序名称、工作中心
 * 这些快照由本服务从工单上抄下来，操作人沿用工序开工时记的那位，工时不填就按工序标准时间算。
 */
@NullMarked
public class WorkOrderTrackingDomainService {

  /** 工时保留位数，与时间列 decimal(18, 2) 对齐 */
  private static final int MINUTE_SCALE = 2;

  /** 报工单工厂 */
  private final WorkOrderTrackingFactory trackingFactory;

  /** 报工单仓储 */
  private final WorkOrderTrackingRepository trackingRepository;

  /** 工单仓储 */
  private final WorkOrderRepository workOrderRepository;

  /**
   * 构造器注入
   *
   * @param trackingFactory 报工单工厂
   * @param trackingRepository 报工单仓储
   * @param workOrderRepository 工单仓储
   */
  public WorkOrderTrackingDomainService(
      WorkOrderTrackingFactory trackingFactory,
      WorkOrderTrackingRepository trackingRepository,
      WorkOrderRepository workOrderRepository) {
    this.trackingFactory = trackingFactory;
    this.trackingRepository = trackingRepository;
    this.workOrderRepository = workOrderRepository;
  }

  /**
   * 创建报工单：建单即累加进工单工序
   *
   * <p>先让工单侧校验通过再建单——反过来的话，工单侧一抛异常就会留下一张「报了工但没汇总」的报工单。
   *
   * @param workOrderId 工单Id
   * @param workOrderOperationId 工单工序Id
   * @param goodQty 本次良品数量
   * @param lossQty 本次报废数量
   * @param workTime 本次工时（分钟），传空则按工序标准时间算
   * @param remark 备注（可空）
   * @return 报工单聚合
   * @throws DomainException 工单或工序行不存在、工序未开工、报工数超过投入数、或按批取时却没有标准批量时抛出
   */
  public WorkOrderTracking create(
      long workOrderId,
      long workOrderOperationId,
      BigDecimal goodQty,
      BigDecimal lossQty,
      @Nullable BigDecimal workTime,
      @Nullable String remark) {
    WorkOrder workOrder = requireWorkOrder(workOrderId);
    WorkOrderOperation operation = requireOperation(workOrder, workOrderOperationId);
    BigDecimal minutes = resolveWorkTime(operation, goodQty, lossQty, workTime);
    workOrder.reportOperation(operation.getId(), goodQty);
    checkReportGate(workOrder, operation, goodQty);
    // 回写：本单生效后的流水合计（现有合计 + 本单）
    workOrder.applyOperationTotals(
        operation.getId(),
        operation.getProducedQty().add(goodQty),
        operation.getActualCycleTime().add(minutes));
    return trackingFactory.create(
        workOrder.getId(),
        workOrder.getNumber(),
        operation.getId(),
        operation.getOperationId(),
        operation.getOperationName(),
        operation.getWorkCenterId(),
        operation.getWorkCenterName(),
        null,
        minutes,
        goodQty,
        lossQty,
        remark);
  }

  /**
   * 作废报工单：把良品、报废与工时从工单工序里减回去
   *
   * <p>与建单同样的顺序：先让工单侧校验通过再作废自己——反过来的话，工单侧一抛异常就会留下一张
   * 「已作废但没减数」的报工单。
   *
   * <p>作废只回到工单工序的汇总数：这张报工单触发过的报工倒冲不会跟着退，要退料由应用层另开一张退料单。
   *
   * @param id 报工单Id
   * @param moment 作废时间
   * @return 报工单聚合（已作废）
   * @throws DomainException 报工单或工单不存在、已作废、后续工序已开工、或工单侧校验不过时抛出
   */
  public WorkOrderTracking voidReport(long id, LocalDateTime moment) {
    WorkOrderTracking model = getExist(id);
    if (model.isVoided()) {
      throw new DomainException("该报工记录已作废，不能重复作废");
    }

    WorkOrder workOrder = requireWorkOrder(model.getWorkOrderId());
    workOrder.unreportOperation(model.getWorkOrderOperationId());

    model.voidReport(moment);
    // 回写：本单作废后的流水合计（现有合计 − 本单）
    WorkOrderOperation operation = requireOperation(workOrder, model.getWorkOrderOperationId());
    workOrder.applyOperationTotals(
        operation.getId(),
        operation.getProducedQty().subtract(model.getGoodQty()),
        operation.getActualCycleTime().subtract(model.getWorkTime()));
    return model;
  }

  /**
   * 报工数量闸：本单生效后的流水合计不得超过本工序的可投入数量
   *
   * <p>可投入数量由工单按前置工序的良品数算，权威数在流水里，这道闸放在按流水汇总的地方——报工领域
   * 服务，而不是工序行（行上不再落库合计）。
   *
   * @param workOrder 工单聚合
   * @param operation 工单工序行（回写之前的瞬时合计）
   * @param goodQty 本次良品数量
   * @param lossQty 本次报废数量
   * @throws DomainException 累计报工数超过可投入数量时抛出
   */
  private void checkReportGate(
      WorkOrder workOrder,
      WorkOrderOperation operation,
      BigDecimal goodQty) {
    BigDecimal total = operation.getProducedQty().add(goodQty);
    BigDecimal availableQty = workOrder.availableQtyFor(operation.getId());
    if (total.compareTo(availableQty) > 0) {
      throw new DomainException(
          "工序" + operation.getOperationName() + "累计报工数" + total + "超过可投入数量" + availableQty);
    }
  }

  /**
   * 按工单查询有效的报工单
   *
   * @param workOrderId 工单Id
   * @return 未作废的报工单列表
   */
  public List<WorkOrderTracking> getEffectiveByWorkOrderId(long workOrderId) {
    return trackingRepository.getListByWorkOrderId(workOrderId).stream()
        .filter(WorkOrderTracking::isEffective)
        .toList();
  }

  // ==================== 私有：取数与工时派生 ====================

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

  /**
   * 定本次工时：现场填了就用现场的，没填就按工序标准时间算
   *
   * <p>产出量取良品加报废——报废件同样吃掉了加工时间。准备工时只在该工序还没有任何报工时补一次，
   * 否则一道分三次报完的工序会把装夹调机算三遍。估算口径复用
   * {@code WorkOrderOperation#estimatedRunMinutes} 与 {@code estimatedSetupMinutes}，按批取时的工序
   * 不足一批也按整批的标准工时计。
   *
   * <p>「第一次」是看工序上已累计的报工量判的。因为建单即累加，连着报两张时第二张已经看得见第一张，
   * 准备工时只会算一次。
   *
   * @param operation 工单工序行
   * @param goodQty 本次良品数量
   * @param lossQty 本次报废数量
   * @param workTime 现场填报工时（可空）
   * @return 本次工时（分钟）
   * @throws DomainException 按批取时却没有标准批量时抛出
   */
  private BigDecimal resolveWorkTime(
      WorkOrderOperation operation,
      BigDecimal goodQty,
      BigDecimal lossQty,
      @Nullable BigDecimal workTime) {
    if (workTime != null) {
      return workTime;
    }
    BigDecimal output = goodQty.add(lossQty);
    BigDecimal minutes = operation.estimatedRunMinutes(output);
    return minutes.setScale(MINUTE_SCALE, RoundingMode.HALF_UP);
  }

  /**
   * 取报工单，不存在则抛异常
   *
   * @param id 报工单Id
   * @return 报工单聚合
   * @throws DomainException 报工单不存在时抛出
   */
  private WorkOrderTracking getExist(long id) {
    WorkOrderTracking model = trackingRepository.get(id);
    if (model == null) {
      throw new DomainException("报工单" + id + "不存在!");
    }
    return model;
  }
}
