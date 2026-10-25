package com.rongia.production.domain.workorder;

import com.rongia.shared.domain.entity.BaseDeleteEntity;
import com.rongia.shared.domain.exception.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Nationalized;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 工单工序
 *
 * <p>工单下达时从工艺路线「快照」生成：工序名、工作中心名与工时都冗余存一份，之后路线改版不影响在制工单。
 *
 * <p>这里只保存工序自己的事实（已完成数量、累计报废、累计工时、实际起止），不保存「计划投入数量」。工序可投入多少 由前置工序的产出决定，是运行时派生值，由 {@code WorkOrder}
 * 计算后传入报工校验——把它在本行存成静态字段， 下达时前置工序还没开工，根本取不到值。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "WorkOrderOperation",
    indexes = {
      @Index(name = "IX_WorkOrderOperation_WorkOrderId", columnList = "WorkOrderId"),
      @Index(name = "IX_WorkOrderOperation_OperationId", columnList = "OperationId"),
      @Index(name = "IX_WorkOrderOperation_WorkCenterId", columnList = "WorkCenterId"),
      @Index(name = "IX_WorkOrderOperation_Sequence", columnList = "Sequence"),
      @Index(name = "IX_WorkOrderOperation_Status", columnList = "Status"),
      // 注意：WorkOrderId + Sequence 的过滤唯一索引带 WHERE IsDeleted=0，JPA 不支持，用 Flyway 创建
    })
public class WorkOrderOperation extends BaseDeleteEntity {

  /** 工单Id */
  @Column(name = "WorkOrderId")
  private Long workOrderId;

  /** 工序Id */
  @Column(name = "OperationId", nullable = false)
  private Long operationId;

  /** 工序名称（快照） */
  @Nationalized
  @Column(name = "OperationName", nullable = false, length = 100)
  private String operationName;

  /** 工作中心Id */
  @Column(name = "WorkCenterId", nullable = false)
  private Long workCenterId;

  /** 工作中心名称（快照） */
  @Nationalized
  @Column(name = "WorkCenterName", nullable = false, length = 200)
  private String workCenterName;

  /** 周期时间（分钟，快照） */
  @Column(name = "CycleTime", nullable = false, precision = 18, scale = 2)
  private BigDecimal cycleTime;

  /** 开工时间（照 Odoo 的 date_start：排产铺定的是计划，开工后就是事实；外协工序为发出时间） */
  @Column(name = "StartTime")
  @Nullable
  private LocalDateTime startTime;

  /** 完工时间（照 Odoo 的 date_finished：铺定的是计划，完工后就是事实） */
  @Column(name = "EndTime")
  @Nullable
  private LocalDateTime endTime;

  /** 实际周期时间（分钟，只读；报工侧按报工单流水汇总回写，照 ERPNext 的 actual_operation_time 口径） */
  @Column(name = "ActualCycleTime", nullable = false, precision = 18, scale = 2)
  private BigDecimal actualCycleTime = BigDecimal.ZERO;

  /** 已完成数量数量（只读；报工侧按报工单流水汇总回写，照 ERPNext 的 completed_qty 口径） */
  @Column(name = "ProducedQty", nullable = false, precision = 18, scale = 2)
  private BigDecimal producedQty = BigDecimal.ZERO;

  /** 生产日期（可空；报工侧按报工单流水取最近一笔回写，照 Odoo 的 production_date 口径） */
  @Column(name = "ProductionDate")
  @Nullable
  private LocalDateTime productionDate;

  /** 工序顺序 */
  @Column(name = "Sequence", nullable = false)
  private Integer sequence;

  /** 状态编码，取值见 {@link WorkOrderOperationStatusEnum} */
  @Nationalized
  @Column(name = "Status", nullable = false, length = 50)
  private String status;

  /**
   * 构造器：从路线快照生成，初始状态为未开始
   *
   * @param operationId 工序Id
   * @param operationName 工序名称（快照）
   * @param workCenterId 工作中心Id
   * @param workCenterName 工作中心名称（快照）
   * @param cycleTime 周期时间（分钟）
   * @param sequence 工序顺序
   */
  WorkOrderOperation(
      long operationId,
      String operationName,
      long workCenterId,
      String workCenterName,
      BigDecimal cycleTime,
      int sequence) {
    apply(operationId, operationName, workCenterId, workCenterName, cycleTime, sequence);
    this.status = WorkOrderOperationStatusEnum.PENDING.getCode();
  }

  /**
   * 本道工序占用的总时长（分钟）
   *
   * <p>准备 + 加工，不含排队——排队是「等上道做完」的时间，用工序顺序体现，重复算会把工期撑大。
   *
   * @param qty 计划数量
   * @return 总时长（分钟）
   */
  public BigDecimal estimatedMinutes(BigDecimal qty) {
    return estimatedRunMinutes(qty);
  }

  /**
   * 标记为可开工
   *
   * <p>首道工序在工单下达时调用，其余工序在上道完工时由工单统一推进。
   */
  void ready() {
    if (!isPending()) {
      return;
    }
    this.status = WorkOrderOperationStatusEnum.READY.getCode();
  }

  /**
   * 收回放行（可开工 → 未开始）
   *
   * <p>上游报工被撤回时，工单把靠这道放行的工序收回，现场就不该再看见一个开不了工的入口。
   */
  void hold() {
    if (!isReady()) {
      return;
    }
    this.status = WorkOrderOperationStatusEnum.PENDING.getCode();
  }

  /**
   * 排产：铺计划开工与计划完工时间
   *
   * @param startTime 计划开工时间
   * @param endTime 计划完工时间
   * @throws DomainException 时间倒挂或工序已开工时抛出
   */
  void schedule(LocalDateTime startTime, LocalDateTime endTime) {
    if (isInProgress() || isCompleted()) {
      throw new DomainException("工序" + this.operationName + "已开工，不能再排产");
    }
    if (endTime.isBefore(startTime)) {
      throw new DomainException("工序" + this.operationName + "计划完工时间早于计划开工时间");
    }
    this.startTime = startTime;
    this.endTime = endTime;
  }

  /**
   * 开工（外协工序即发料给供应商）
   *
   * @param startTime 实际开工时间
   * @throws DomainException 当前状态不允许开工时抛出
   */
  public void start(LocalDateTime startTime) {
    if (!isReady()) {
      throw new DomainException("工序" + this.operationName + "当前状态不允许开工");
    }
    this.status = WorkOrderOperationStatusEnum.IN_PROGRESS.getCode();
    this.startTime = startTime;
  }

  /**
   * 报工校验：工序必须开工、本次数量必须非负
   *
   * <p>合计不在这里累加——良品、报废、工时的权威数在报工单流水里，由报工侧按流水汇总回写
   * {@code applyOperationTotals}；本方法只把关（照 ERPNext：校验在工序行、记账在明细）。
   *
   * @param producedQty 本次完成数量
   * @throws DomainException 工序状态不允许或数量为负时抛出
   */
  public void report(BigDecimal producedQty) {
    if (!isInProgress()) {
      throw new DomainException("工序" + this.operationName + "未开工，不能报工");
    }
    if (producedQty.signum() < 0) {
      throw new DomainException("报工数量不能为负数");
    }
  }

  /**
   * 撤回报工的行级校验与状态回退
   *
   * <p>数量回退不在这里做——权威数在报工单流水里，由报工侧重算回写。这里只负责：工序被撤空时退回加工中
   * 并清掉完工时间（照 ERPNext 的作业卡取消节奏）。
   *
   * @throws DomainException 工序未开工时抛出
   */
  void unreport() {
    if (!isInProgress() && !isCompleted()) {
      throw new DomainException("工序" + this.operationName + "未开工，不能撤回报工");
    }
  }

  /**
   * 回写流水汇总的合计（落库的只读列，报工侧按流水汇总后回写）
   *
   * <p>由报工侧按报工单流水汇总后调用：报工单过账、作废之后各跑一遍，行上的状态推进（撤空退回加工中）也在这
   * 一并做掉。报废不落行（照 Odoo 的 qty_produced 单一口径，报废的权威数在报工单流水里），生产日期取流水
   * 最近一笔的业务日期。
   *
   * @param producedQty 流水已完成数量
   * @param actualCycleTime 流水累计工时
   * @param productionDate 生产日期（可空）
   */
  void applyOperationTotals(
      BigDecimal producedQty, BigDecimal actualCycleTime, @Nullable LocalDateTime productionDate) {
    this.producedQty = producedQty;
    this.actualCycleTime = actualCycleTime;
    this.productionDate = productionDate;
    if (isCompleted() && producedQty.signum() == 0) {
      this.status = WorkOrderOperationStatusEnum.IN_PROGRESS.getCode();
      this.endTime = null;
    }
  }

  /**
   * 完工
   *
   * @param endTime 实际完工时间
   * @throws DomainException 未开工、或外协工序未回厂时抛出
   */
  void complete(LocalDateTime endTime) {
    if (!isInProgress()) {
      throw new DomainException("工序" + this.operationName + "未开工，不能完工");
    }
    this.status = WorkOrderOperationStatusEnum.COMPLETED.getCode();
    this.endTime = endTime;
  }

  /**
   * 跳过
   *
   * <p>把这道工序标记为不执行，后续工序照常推进。
   */
  void skip() {
    this.status = WorkOrderOperationStatusEnum.SKIPPED.getCode();
  }

  /**
   * 是否已结束（完工或跳过）
   *
   * @return true 表示已结束
   */
  boolean isFinished() {
    return isCompleted() || isSkipped();
  }

  /**
   * 是否已经报过工（已完成数量加报废不为零）
   *
   * <p>撤回前面的报工要看后面这道工序有没有用掉料，状态说了不算——它可能刚被撤回退回加工中。
   *
   * @return true 表示累计报工数不为零
   */
  boolean hasReported() {
    return this.producedQty.signum() != 0;
  }

  /**
   * 是否未开始
   *
   * @return true 表示未开始
   */
  boolean isPending() {
    return WorkOrderOperationStatusEnum.PENDING.getCode().equals(this.status);
  }

  /**
   * 是否可开工
   *
   * @return true 表示可开工
   */
  boolean isReady() {
    return WorkOrderOperationStatusEnum.READY.getCode().equals(this.status);
  }

  /**
   * 是否加工中（外协工序表示已发出待回厂）
   *
   * @return true 表示加工中
   */
  boolean isInProgress() {
    return WorkOrderOperationStatusEnum.IN_PROGRESS.getCode().equals(this.status);
  }

  /**
   * 是否已完工
   *
   * @return true 表示已完工
   */
  boolean isCompleted() {
    return WorkOrderOperationStatusEnum.COMPLETED.getCode().equals(this.status);
  }

  /**
   * 是否已跳过
   *
   * @return true 表示已跳过
   */
  boolean isSkipped() {
    return WorkOrderOperationStatusEnum.SKIPPED.getCode().equals(this.status);
  }

  // ==================== 估算：工时与成本 ====================

  /**
   * 估算周期时间（分钟）
   *
   * <p>手工模式下 {@code cycleTime} 是固定工时，乘以数量；自动模式应取历史工单平均， 但历史取数需要仓储支持，本版本按固定工时兜底。
   *
   * @param qty 计划数量
   * @return 估算周期时间（分钟）
   * @throws DomainException 数量为负时抛出
   */
  public BigDecimal estimatedRunMinutes(BigDecimal qty) {
    checkQty(qty);
    return this.cycleTime.multiply(qty);
  }

  // ==================== 私有：校验与赋值 ====================

  /**
   * 快照字段的校验与赋值（构造时调用一次）
   *
   * @param operationId 工序Id
   * @param operationName 工序名称（快照）
   * @param workCenterId 工作中心Id
   * @param workCenterName 工作中心名称（快照）
   * @param cycleTime 周期时间（分钟）
   * @param sequence 工序顺序
   * @throws DomainException 快照字段非法时抛出
   */
  private void apply(
      long operationId,
      String operationName,
      long workCenterId,
      String workCenterName,
      BigDecimal cycleTime,
      int sequence) {
    if (sequence <= 0) {
      throw new DomainException("工序顺序必须大于0");
    }
    if (operationId <= 0) {
      throw new DomainException("工序不能为空");
    }
    if (workCenterId <= 0) {
      throw new DomainException("工作中心不能为空");
    }
    String trimmedName = StringUtils.trimToNull(operationName);
    String trimmedWorkCenterName = StringUtils.trimToNull(workCenterName);
    if (trimmedName == null) {
      throw new DomainException("工序名称不能为空");
    }
    if (trimmedWorkCenterName == null) {
      throw new DomainException("工作中心名称不能为空");
    }
    validateNonNegative("周期时间", cycleTime);

    this.operationId = operationId;
    this.operationName = trimmedName;
    this.workCenterId = workCenterId;
    this.workCenterName = trimmedWorkCenterName;
    this.cycleTime = cycleTime;
    this.sequence = sequence;
  }

  /**
   * 校验工时字段非负
   *
   * <p>参数类型在 {@code @NullMarked} 下即非空契约，调用方必须保证不为 {@code null}，因此这里只判负数。
   *
   * @param field 字段中文名，用于拼装错误提示
   * @param value 待校验的数值
   * @throws DomainException 值为负数时抛出
   */
  private void validateNonNegative(String field, BigDecimal value) {
    if (value.signum() < 0) {
      throw new DomainException(field + "不能为负数");
    }
  }

  /**
   * 校验参与估算的数量
   *
   * @param qty 计划数量
   * @throws DomainException 数量为负时抛出
   */
  private void checkQty(BigDecimal qty) {
    if (qty.signum() < 0) {
      throw new DomainException("计划数量不能为负数");
    }
  }
}
