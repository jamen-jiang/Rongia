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

  /** 准备时间（分钟，每批一次，快照，照 carbon 的 setupTime） */
  @Column(name = "SetupTime", nullable = false, precision = 18, scale = 2)
  private BigDecimal setupTime;

  /** 人工时间（分钟/件，快照，照 carbon 的 laborTime） */
  @Column(name = "LaborTime", nullable = false, precision = 18, scale = 2)
  private BigDecimal laborTime;

  /** 机器时间（分钟/件，快照，照 carbon 的 machineTime） */
  @Column(name = "MachineTime", nullable = false, precision = 18, scale = 2)
  private BigDecimal machineTime;

  /** 人工费率（元/小时，快照，照 carbon 的 laborRate）：下达时中心的人工费率，之后调价不影响在制工单 */
  @Column(name = "LaborRate", nullable = false, precision = 18, scale = 2)
  private BigDecimal laborRate;

  /** 机器费率（元/小时，快照，照 carbon 的 machineRate）：乘机器时间，算设备这一段成本 */
  @Column(name = "MachineRate", nullable = false, precision = 18, scale = 2)
  private BigDecimal machineRate;

  /** 制造费用率（元/小时，快照，照 carbon 的 overheadRate）：乘总工时，摊厂房与管理开销 */
  @Column(name = "OverheadRate", nullable = false, precision = 18, scale = 2)
  private BigDecimal overheadRate;

  /** 本工序应完成时间（从工单交期沿工序链反推，照 carbon 的 dueDate；工单没有交期时为空） */
  @Column(name = "DeliveryTime")
  @Nullable
  private LocalDateTime deliveryTime;

  /** 计划开工时间（排产铺定，照 carbon 的 startDate） */
  @Column(name = "PlannedStartTime")
  @Nullable
  private LocalDateTime plannedStartTime;

  /** 预计完工时间（排产按工时算出来的结论，照 carbon 的 projectedEndTime） */
  @Column(name = "ProjectedEndTime")
  @Nullable
  private LocalDateTime projectedEndTime;

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
   * @param setupTime 准备时间（分钟，每批一次）
   * @param laborTime 人工时间（分钟/件）
   * @param machineTime 机器时间（分钟/件）
   * @param laborRate 人工费率
   * @param machineRate 机器费率
   * @param overheadRate 制造费用率
   * @param sequence 工序顺序
   */
  WorkOrderOperation(
      long operationId,
      String operationName,
      long workCenterId,
      String workCenterName,
      BigDecimal setupTime,
      BigDecimal laborTime,
      BigDecimal machineTime,
      BigDecimal laborRate,
      BigDecimal machineRate,
      BigDecimal overheadRate,
      int sequence) {
    apply(
        operationId,
        operationName,
        workCenterId,
        workCenterName,
        setupTime,
        laborTime,
        machineTime,
        laborRate,
        machineRate,
        overheadRate,
        sequence);
    this.status = WorkOrderOperationStatusEnum.PENDING.getCode();
  }

  /**
   * 本道工序占用的总时长（分钟）
   *
   * <p>准备按每批一次记，人工与机器按件累加：准备 + (人工 + 机器) × 数量。不含排队——排队是「等上道做完」的时间，
   * 用工序顺序体现，重复算会把工期撑大。
   *
   * @param qty 计划数量
   * @return 总时长（分钟）
   * @throws DomainException 数量为负时抛出
   */
  public BigDecimal estimatedMinutes(BigDecimal qty) {
    checkQty(qty);
    return this.setupTime.add(this.laborTime.add(this.machineTime).multiply(qty));
  }

  /**
   * 排产：铺计划开工与预计完工
   *
   * <p>两个都是算出来的：开工是从工单开工顺着工序链推的，完工是再加工时。实际干到哪儿不在这里——
   * 那由报工流水和状态体现。
   *
   * @param plannedStartTime 计划开工时间
   * @param projectedEndTime 预计完工时间
   * @throws DomainException 时间倒挂或工序已开工时抛出
   */
  void schedule(LocalDateTime plannedStartTime, LocalDateTime projectedEndTime) {
    if (isInProgress() || isCompleted()) {
      throw new DomainException("工序" + this.operationName + "已开工，不能再排产");
    }
    if (projectedEndTime.isBefore(plannedStartTime)) {
      throw new DomainException("工序" + this.operationName + "预计完工早于计划开工");
    }
    this.plannedStartTime = plannedStartTime;
    this.projectedEndTime = projectedEndTime;
  }

  /**
   * 回填本工序的应完成时间
   *
   * <p>不是排产铺的，是从工单交期倒推出来的：要让整张单按时交，这道工序最晚什么时候得做完。
   * 工单没有交期时传空。
   *
   * @param deliveryTime 本工序应完成时间（可空）
   */
  void fillDeliveryTime(@Nullable LocalDateTime deliveryTime) {
    this.deliveryTime = deliveryTime;
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
   * 报废件不产出下道能吃的东西，它只算工时。撤回把状态退回去。
   *
   * @param producedQty 流水已完成数量
   * @param availableQty 本工序可投入数量
   */
  void applyOperationTotals(
      BigDecimal producedQty,
      BigDecimal availableQty) {
    this.producedQty = producedQty;
    if (producedQty.signum() == 0) {
      this.status = WorkOrderOperationStatusEnum.PENDING.getCode();
      return;
    }
    if (producedQty.compareTo(availableQty) >= 0) {
      this.status = WorkOrderOperationStatusEnum.COMPLETED.getCode();
      return;
    }
    this.status = WorkOrderOperationStatusEnum.WORK_IN_PROGRESS.getCode();
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

  // ==================== 私有：校验与赋值 ====================

  /**
   * 快照字段的校验与赋值（构造时调用一次）
   *
   * @param operationId 工序Id
   * @param operationName 工序名称（快照）
   * @param workCenterId 工作中心Id
   * @param workCenterName 工作中心名称（快照）
   * @param setupTime 准备时间（分钟，每批一次）
   * @param laborTime 人工时间（分钟/件）
   * @param machineTime 机器时间（分钟/件）
   * @param laborRate 人工费率
   * @param machineRate 机器费率
   * @param overheadRate 制造费用率
   * @param sequence 工序顺序
   * @throws DomainException 快照字段非法时抛出
   */
  private void apply(
      long operationId,
      String operationName,
      long workCenterId,
      String workCenterName,
      BigDecimal setupTime,
      BigDecimal laborTime,
      BigDecimal machineTime,
      BigDecimal laborRate,
      BigDecimal machineRate,
      BigDecimal overheadRate,
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
    validateNonNegative("准备时间", setupTime);
    validateNonNegative("人工时间", laborTime);
    validateNonNegative("机器时间", machineTime);
    validateNonNegative("人工费率", laborRate);
    validateNonNegative("机器费率", machineRate);
    validateNonNegative("制造费用率", overheadRate);

    this.operationId = operationId;
    this.operationName = trimmedName;
    this.workCenterId = workCenterId;
    this.workCenterName = trimmedWorkCenterName;
    this.setupTime = setupTime;
    this.laborTime = laborTime;
    this.machineTime = machineTime;
    this.laborRate = laborRate;
    this.machineRate = machineRate;
    this.overheadRate = overheadRate;
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
