package com.rongia.production.domain.jobcard;

import com.rongia.shared.domain.aggregate.OrderAggregateRoot;
import com.rongia.shared.domain.enums.SerialNumberTypeEnum;
import com.rongia.shared.domain.exception.DomainException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.SQLRestriction;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 工卡（作业卡）
 *
 * <p>一张工卡对应工单上的一道工序，是车间干活的那张纸（照 ERPNext 的 Job Card）：卡上写计划做多少、
 * 已经做了多少、废了多少、花了多少工时，卡下挂着一笔一笔的工时日志。
 *
 * <p>报工不另建单据——每一枪就是往这张卡上加一条工时日志，卡头的合计按有效日志累加。作废某一枪就把那条
 * 日志作废，卡头跟着减回去；整张卡不要了就取消，留痕不删。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "JobCard",
    indexes = {
      @Index(name = "IX_JobCard_WorkOrderId", columnList = "WorkOrderId"),
      @Index(name = "IX_JobCard_WorkOrderOperationId", columnList = "WorkOrderOperationId"),
      @Index(name = "IX_JobCard_Status", columnList = "Status")
    })
public class JobCard extends OrderAggregateRoot {

  /** 工单Id */
  @Column(name = "WorkOrderId", nullable = false)
  private Long workOrderId;

  /** 工单号（快照） */
  @Nationalized
  @Column(name = "WorkOrderNumber", nullable = false, length = 30)
  private String workOrderNumber;

  /** 工单工序Id */
  @Column(name = "WorkOrderOperationId", nullable = false)
  private Long workOrderOperationId;

  /** 工序名称（快照） */
  @Nationalized
  @Column(name = "OperationName", nullable = false, length = 100)
  private String operationName;

  /** 计划数量（这张卡要做多少） */
  @Column(name = "PlannedQty", nullable = false, precision = 18, scale = 2)
  private BigDecimal plannedQty;

  /** 已完成数量（按有效日志汇总） */
  @Column(name = "CompletedQty", nullable = false, precision = 18, scale = 2)
  private BigDecimal completedQty = BigDecimal.ZERO;

  /** 报废数量（报工时累加，不挂在工时行上） */
  @Column(name = "LossQty", nullable = false, precision = 18, scale = 2)
  private BigDecimal lossQty = BigDecimal.ZERO;

  /** 总工时（分钟，按有效日志汇总） */
  @Column(name = "TotalMinutes", nullable = false, precision = 18, scale = 2)
  private BigDecimal totalMinutes = BigDecimal.ZERO;

  /** 实际开工时间（第一笔报工落） */
  @Column(name = "StartTime")
  @Nullable
  private LocalDateTime startTime;

  /** 实际完工时间（报满计划数量落） */
  @Column(name = "EndTime")
  @Nullable
  private LocalDateTime endTime;

  /** 状态编码，取值见 {@link JobCardStatusEnum} */
  @Nationalized
  @Column(name = "Status", nullable = false, length = 50)
  private String status;

  /** 备注（可空） */
  @Nationalized
  @Column(name = "Remark", length = 500)
  @Nullable
  private String remark;

  /** 工时日志（一笔一行） */
  @OneToMany(
      fetch = FetchType.EAGER,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE},
      orphanRemoval = true)
  @JoinColumn(name = "JobCardId")
  @SQLRestriction("IsDeleted = 0")
  private List<JobCardTimeLog> timeLogs = new ArrayList<>();

  /**
   * 构造器
   *
   * @param workOrderId 工单Id
   * @param workOrderNumber 工单号（快照）
   * @param workOrderOperationId 工单工序Id
   * @param operationName 工序名称（快照）
   * @param plannedQty 计划数量
   * @param remark 备注（可空）
   * @param number 单号（传空串表示由 OrderAddedEvent 的监听器后续分配）
   * @throws DomainException 工单、工序、计划数量非法时抛出
   */
  JobCard(
      long workOrderId,
      String workOrderNumber,
      long workOrderOperationId,
      String operationName,
      BigDecimal plannedQty,
      @Nullable String remark,
      String number) {
    super(number);
    if (workOrderId <= 0) {
      throw new DomainException("工单不能为空");
    }
    if (workOrderOperationId <= 0) {
      throw new DomainException("工单工序不能为空");
    }
    if (plannedQty.signum() <= 0) {
      throw new DomainException("计划数量必须大于0");
    }
    String trimmedNumber = StringUtils.trimToNull(workOrderNumber);
    String trimmedName = StringUtils.trimToNull(operationName);
    if (trimmedNumber == null) {
      throw new DomainException("工单号不能为空");
    }
    if (trimmedName == null) {
      throw new DomainException("工序名称不能为空");
    }
    this.workOrderId = workOrderId;
    this.workOrderNumber = trimmedNumber;
    this.workOrderOperationId = workOrderOperationId;
    this.operationName = trimmedName;
    this.plannedQty = plannedQty;
    this.remark = remark;
    this.status = JobCardStatusEnum.PENDING.getCode();
  }

  /**
   * 流水号类型
   *
   * @return 工卡
   */
  @Override
  public SerialNumberTypeEnum getSerialNumberType() {
    return SerialNumberTypeEnum.JOB_CARD;
  }

  /**
   * 获取工时日志（只读）
   *
   * @return 不可变列表
   */
  public List<JobCardTimeLog> getTimeLogs() {
    return Collections.unmodifiableList(timeLogs);
  }

  /**
   * 加一笔报工
   *
   * <p>一条工时日志 + 卡头累加。第一笔落开工时间；卡还没开工就转加工中，报满计划数量也不会自动完成——
   * 完成是提交那一下的事（照 ERPNext：没提交前一直能改，提交后才算数）。
   *
   * @param fromTime 开始时间（可空，一枪报完没有起止）
   * @param toTime 结束时间（可空）
   * @param minutes 本次工时（分钟）
   * @param completedQty 本次良品数量
   * @param remark 备注（可空）
   * @param moment 报工时刻
   * @throws DomainException 卡已提交或已取消时抛出
   */
  public void addTimeLog(
      @Nullable LocalDateTime fromTime,
      @Nullable LocalDateTime toTime,
      BigDecimal minutes,
      BigDecimal completedQty,
      @Nullable String remark,
      LocalDateTime moment) {
    ensureEditable("报工");
    timeLogs.add(new JobCardTimeLog(fromTime, toTime, minutes, completedQty, remark));

    this.completedQty = this.completedQty.add(completedQty);
    this.totalMinutes = this.totalMinutes.add(minutes);
    if (this.startTime == null) {
      this.startTime = moment;
    }
    this.status = JobCardStatusEnum.WORK_IN_PROGRESS.getCode();
  }

  /**
   * 记一笔报废（仅未提交的卡可记）
   *
   * <p>报废不挂在工时行上（照 ERPNext：工时行只记良品），它是卡头上的一个总数，报工时报多少累多少。
   *
   * @param qty 本次报废数量
   * @throws DomainException 卡已提交或已取消、或数量为负时抛出
   */
  public void addLossQty(BigDecimal qty) {
    ensureEditable("记报废");
    if (qty.signum() < 0) {
      throw new DomainException("报废数量不能为负数");
    }
    this.lossQty = this.lossQty.add(qty);
  }

  /**
   * 删掉一笔报工（仅未提交的卡可删）
   *
   * <p>照 ERPNext：没提交前直接删行，卡头的良品与工时减回去；报废是卡头上的总数，不随行删。
   *
   * @param timeLogId 工时日志Id
   * @throws DomainException 卡已提交或已取消、或日志不存在时抛出
   */
  public void deleteTimeLog(long timeLogId) {
    ensureEditable("删报工行");
    JobCardTimeLog log =
        timeLogs.stream()
            .filter(l -> Objects.equals(l.getId(), timeLogId))
            .findFirst()
            .orElseThrow(() -> new DomainException("工时日志" + timeLogId + "不存在!"));
    timeLogs.remove(log);
    this.completedQty = this.completedQty.subtract(log.getCompletedQty());
    this.totalMinutes = this.totalMinutes.subtract(log.getTimeInMinutes());
    if (timeLogs.isEmpty()) {
      this.status = JobCardStatusEnum.PENDING.getCode();
      this.startTime = null;
      this.endTime = null;
    }
  }

  /**
   * 提交：卡锁死，之后不能再加删报工行
   *
   * @param moment 提交时刻
   * @throws DomainException 卡已提交/已取消、或一笔报工都没有时抛出
   */
  public void submit(LocalDateTime moment) {
    if (isCompleted() || isCancelled()) {
      throw new DomainException("工卡" + getNumber() + "已提交或已取消");
    }
    if (timeLogs.isEmpty()) {
      throw new DomainException("工卡" + getNumber() + "一笔报工都没有，不能提交");
    }
    this.status = JobCardStatusEnum.COMPLETED.getCode();
    this.endTime = this.endTime == null ? moment : this.endTime;
  }

  /**
   * 取消整张工卡
   *
   * <p>卡上已经报过的账一并作废：有效日志逐条标作废、卡头合计清零、起止清空。工序行那边由
   * {@link JobCardDomainService} 按取消前的合计减回去——取消一张卡不等于这道工序不做了，
   * 之后可以重新建一张卡接着报。
   *
   * @param moment 取消时间
   * @throws DomainException 已取消时抛出
   */
  public void cancel(LocalDateTime moment) {
    if (isCancelled()) {
      throw new DomainException("工卡" + getNumber() + "已取消");
    }
    this.status = JobCardStatusEnum.CANCELLED.getCode();
    this.endTime = this.endTime == null ? moment : this.endTime;
  }

  /**
   * 是否未开始
   *
   * @return true 表示未开始
   */
  public boolean isPending() {
    return JobCardStatusEnum.PENDING.getCode().equals(this.status);
  }

  /**
   * 是否加工中
   *
   * @return true 表示加工中
   */
  public boolean isInProgress() {
    return JobCardStatusEnum.WORK_IN_PROGRESS.getCode().equals(this.status);
  }

  /**
   * 是否已完成（已提交）
   *
   * @return true 表示已完成
   */
  public boolean isCompleted() {
    return JobCardStatusEnum.COMPLETED.getCode().equals(this.status);
  }

  /**
   * 是否已取消
   *
   * @return true 表示已取消
   */
  public boolean isCancelled() {
    return JobCardStatusEnum.CANCELLED.getCode().equals(this.status);
  }

  // ==================== 私有：校验 ====================

  /**
   * 校验卡还能改报工行：提交或取消之后整张卡锁死
   *
   * @param action 动作名称，用于报错文案
   * @throws DomainException 已提交或已取消时抛出
   */
  private void ensureEditable(String action) {
    if (isCompleted()) {
      throw new DomainException("工卡" + getNumber() + "已提交，不能再" + action);
    }
    if (isCancelled()) {
      throw new DomainException("工卡" + getNumber() + "已取消，不能再" + action);
    }
  }
}
