package com.rongia.production.domain.workorder;

import com.rongia.shared.domain.entity.BaseDeleteEntity;
import com.rongia.shared.domain.exception.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
 * 计算后传入报工校验——把它在本行存成静态字段， 下达时前置工序还没报工，根本取不到值。
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

  /** 批量大小（件，快照）：一次投产多少件，0 表示不分批 */
  @Column(name = "BatchSize", nullable = false, precision = 18, scale = 2)
  private BigDecimal batchSize;

  /** 小时费率（元/小时，快照）：下达时的中心费率，之后中心调价不影响在制工单 */
  @Column(name = "HourlyRate", nullable = false, precision = 18, scale = 2)
  private BigDecimal hourlyRate;

  /** 计划开工时间（排产铺定，照 ERPNext 的 planned_start_time） */
  @Column(name = "PlannedStartTime")
  @Nullable
  private LocalDateTime plannedStartTime;

  /** 计划完工时间（排产铺定，照 ERPNext 的 planned_end_time） */
  @Column(name = "PlannedEndTime")
  @Nullable
  private LocalDateTime plannedEndTime;

  /** 实际开工时间（第一笔报工时落，照 ERPNext 的 actual_start_time；外协工序为发出时间） */
  @Column(name = "ActualStartTime")
  @Nullable
  private LocalDateTime actualStartTime;

  /** 实际完工时间（累计良品报满可投入量时落，照 ERPNext 的 actual_end_time） */
  @Column(name = "ActualEndTime")
  @Nullable
  private LocalDateTime actualEndTime;

  /** 实际周期时间（分钟，只读；报工侧按报工单流水汇总回写，照 ERPNext 的 actual_operation_time 口径） */
  @Column(name = "ActualCycleTime", nullable = false, precision = 18, scale = 2)
  private BigDecimal actualCycleTime = BigDecimal.ZERO;

  /** 已完成数量数量（只读；报工侧按报工单流水汇总回写，照 ERPNext 的 completed_qty 口径） */
  @Column(name = "ProducedQty", nullable = false, precision = 18, scale = 2)
  private BigDecimal producedQty = BigDecimal.ZERO;

  /** 工序顺序 */
  @Column(name = "Sequence", nullable = false)
  private Integer sequence;

  /** 状态编码，取值见 {@link WorkOrderOperationStatusEnum}：按累计良品对可投入量算出来，不单独改 */
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
   * @param batchSize 批量大小（件）
   * @param hourlyRate 小时费率
   * @param sequence 工序顺序
   */
  WorkOrderOperation(
      long operationId,
      String operationName,
      long workCenterId,
      String workCenterName,
      BigDecimal cycleTime,
      BigDecimal batchSize,
      BigDecimal hourlyRate,
      int sequence) {
    apply(
        operationId,
        operationName,
        workCenterId,
        workCenterName,
        cycleTime,
        batchSize,
        hourlyRate,
        sequence);
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
   * 排产：铺计划开工与计划完工时间
   *
   * <p>只写计划两列，不碰实际列——原计划与实际执行各存各的，事后才查得出拖了多久。
   *
   * @param plannedStartTime 计划开工时间
   * @param plannedEndTime 计划完工时间
   * @throws DomainException 时间倒挂或工序已开工时抛出
   */
  void schedule(LocalDateTime plannedStartTime, LocalDateTime plannedEndTime) {
    if (isInProgress() || isCompleted()) {
      throw new DomainException("工序" + this.operationName + "已开工，不能再排产");
    }
    if (plannedEndTime.isBefore(plannedStartTime)) {
      throw new DomainException("工序" + this.operationName + "计划完工时间早于计划开工时间");
    }
    this.plannedStartTime = plannedStartTime;
    this.plannedEndTime = plannedEndTime;
  }

  /**
   * 报工校验：本次数量必须非负
   *
   * <p>不要求先开工：工序状态由累计良品对可投入量算出来，报第一笔就是开工。合计不在这里累加——良品、报废、
   * 工时的权威数在报工单流水里，由报工侧按流水汇总回写 {@code applyOperationTotals}。
   *
   * @param producedQty 本次完成数量
   * @throws DomainException 数量为负时抛出
   */
  public void report(BigDecimal producedQty) {
    if (producedQty.signum() < 0) {
      throw new DomainException("报工数量不能为负数");
    }
  }

  /**
   * 回写流水汇总的合计，并按数量定状态（照 ERPNext 的 update_operation_status）
   *
   * <p>没有「点完工」：累计良品为 0 是未开始，小于可投入量是加工中，够了就是已完工。报废不进这个判据——
   * 报废件不产出下道能吃的东西，它只算工时。实际起止时间跟着第一笔报工与报满那一刻落，撤回把状态退回去。
   *
   * @param producedQty 流水已完成数量
   * @param actualCycleTime 流水累计工时
   * @param availableQty 本工序可投入数量
   * @param moment 报工时刻
   */
  void applyOperationTotals(
      BigDecimal producedQty,
      BigDecimal actualCycleTime,
      BigDecimal availableQty,
      LocalDateTime moment) {
    this.producedQty = producedQty;
    this.actualCycleTime = actualCycleTime;
    if (producedQty.signum() == 0) {
      this.status = WorkOrderOperationStatusEnum.PENDING.getCode();
      this.actualStartTime = null;
      this.actualEndTime = null;
      return;
    }
    if (this.actualStartTime == null) {
      this.actualStartTime = moment;
    }
    if (producedQty.compareTo(availableQty) >= 0) {
      this.status = WorkOrderOperationStatusEnum.COMPLETED.getCode();
      this.actualEndTime = this.actualEndTime == null ? moment : this.actualEndTime;
      return;
    }
    this.status = WorkOrderOperationStatusEnum.WORK_IN_PROGRESS.getCode();
    this.actualEndTime = null;
  }

  /**
   * 是否已经报过工（已完成数量不为零）
   *
   * <p>撤回前面的报工要看后面这道工序有没有用掉料，状态说了不算——状态是按量算的。
   *
   * @return true 表示累计报工数不为零
   */
  boolean hasReported() {
    return this.producedQty.signum() != 0;
  }

  /**
   * 是否加工中（外协工序表示已发出待回厂）
   *
   * @return true 表示加工中
   */
  boolean isInProgress() {
    return WorkOrderOperationStatusEnum.WORK_IN_PROGRESS.getCode().equals(this.status);
  }

  /**
   * 是否已完工
   *
   * @return true 表示已完工
   */
  boolean isCompleted() {
    return WorkOrderOperationStatusEnum.COMPLETED.getCode().equals(this.status);
  }

  // ==================== 估算：工时与成本 ====================

  /**
   * 估算周期时间（分钟）
   *
   * <p>一次循环做 {@code batchSize} 件，所以循环次数是「数量 ÷ 批量」向上取整，再乘单次循环工时。批量 0 表示不分批，
   * 按单件循环算。{@code cycleTime} 是固定工时，取历史平均需要仓储支持，本版本按固定工时兜底。
   *
   * @param qty 计划数量
   * @return 估算周期时间（分钟）
   * @throws DomainException 数量为负时抛出
   */
  public BigDecimal estimatedRunMinutes(BigDecimal qty) {
    checkQty(qty);
    if (this.batchSize.signum() <= 0) {
      return this.cycleTime.multiply(qty);
    }
    BigDecimal cycles = qty.divide(this.batchSize, 0, RoundingMode.CEILING);
    return this.cycleTime.multiply(cycles);
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
   * @param batchSize 批量大小（件）
   * @param hourlyRate 小时费率
   * @param sequence 工序顺序
   * @throws DomainException 快照字段非法时抛出
   */
  private void apply(
      long operationId,
      String operationName,
      long workCenterId,
      String workCenterName,
      BigDecimal cycleTime,
      BigDecimal batchSize,
      BigDecimal hourlyRate,
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
    validateNonNegative("批量大小", batchSize);
    validateNonNegative("小时费率", hourlyRate);

    this.operationId = operationId;
    this.operationName = trimmedName;
    this.workCenterId = workCenterId;
    this.workCenterName = trimmedWorkCenterName;
    this.cycleTime = cycleTime;
    this.batchSize = batchSize;
    this.hourlyRate = hourlyRate;
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
