package com.rongia.production.domain.workordertracking;

import com.rongia.shared.domain.aggregate.UpdateAggregateRoot;
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
 * 工单跟踪单
 *
 * <p>为什么独立成聚合而不是给工单工序加几个字段：报工是「一次事件」，天然带操作人、 本次良品/报废与工时。拍平成一个累计值之后，计件工资、人工成本、异常分析全部无从追溯。
 *
 * <p>现场只该填三样：工序、良品数、报废数。工单号、工序名称、工作中心这些快照由领域服务 从工单上抄，工时不填就按工序的标准时间算——让人抄一遍就等于给自己留一个对不上的机会。
 *
 * <p>建单即提交：这张单没有草稿。现场扫完工单号落下三个数，当场就累加进工单工序的汇总，校验也在那一刻发生
 * ——错了当场报错，而不是留到下班对账才发现。要改数只有一条路：作废这一张，重新报一张；作废会把当初累加 进去的良品、报废与工时从工序上减回来，单据本身留着当痕迹。
 *
 * <p>所以这里没有状态机，只有一列「作废时间」：为空表示这笔报工还算数，有值表示已经冲回去了。报工不动库存， 因此不必像库存单据那样另开一张红字单——作废与红冲是同一件事，这张单自己就能承载。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "WorkOrderTracking",
    indexes = {
      @Index(name = "IX_WorkOrderTracking_WorkOrderId", columnList = "WorkOrderId"),
      @Index(
          name = "IX_WorkOrderTracking_WorkOrderOperationId",
          columnList = "WorkOrderOperationId"),
      @Index(name = "IX_WorkOrderTracking_OperatorId", columnList = "OperatorId")
    })
public class WorkOrderTracking extends UpdateAggregateRoot {

  /** 工单Id */
  @Column(name = "WorkOrderId", nullable = false)
  private Long workOrderId;

  /** 工单号（快照，由领域服务从工单抄） */
  @Nationalized
  @Column(name = "WorkOrderNumber", nullable = false, length = 30)
  private String workOrderNumber;

  /** 工单工序Id */
  @Column(name = "WorkOrderOperationId", nullable = false)
  private Long workOrderOperationId;

  /** 工序Id（快照） */
  @Column(name = "OperationId", nullable = false)
  private Long operationId;

  /** 工序名称（快照） */
  @Nationalized
  @Column(name = "OperationName", nullable = false, length = 100)
  private String operationName;

  /** 工作中心Id（快照） */
  @Column(name = "WorkCenterId", nullable = false)
  private Long workCenterId;

  /** 工作中心名称（快照） */
  @Nationalized
  @Column(name = "WorkCenterName", nullable = false, length = 100)
  private String workCenterName;

  /** 操作人Id（可空，默认沿用工序开工时记的人） */
  @Column(name = "OperatorId")
  @Nullable
  private Long operatorId;

  /** 本次工时（分钟，不填则按工序标准时间派生） */
  @Column(name = "WorkTime", nullable = false, precision = 18, scale = 2)
  private BigDecimal workTime;

  /** 本次良品数量 */
  @Column(name = "GoodQty", nullable = false, precision = 18, scale = 2)
  private BigDecimal goodQty;

  /** 本次报废数量 */
  @Column(name = "LossQty", nullable = false, precision = 18, scale = 2)
  private BigDecimal lossQty;

  /** 作废时间（为空表示这笔报工还算数） */
  @Column(name = "VoidedTime")
  @Nullable
  private LocalDateTime voidedTime;

  /** 备注（可空） */
  @Nationalized
  @Column(name = "Remark", length = 500)
  @Nullable
  private String remark;

  /**
   * 构造器
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
   */
  WorkOrderTracking(
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
      @Nullable String remark) {
    apply(
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
        remark);
  }

  /**
   * 作废：把这一笔报工冲回去
   *
   * <p>只负责自己这一半；把良品、报废与工时从工单工序汇总里减回去由 {@code WorkOrderTrackingDomainService}
   * 编排，两下必须成对，否则累计值与单上之数就对不上。
   *
   * <p>作废不是删除：这张单留着，说明「当时有人在这里报过、后来冲掉了」。要再报就另开一张。
   *
   * @param moment 作废时间
   * @throws DomainException 已作废时抛出
   */
  void voidReport(LocalDateTime moment) {
    if (isVoided()) {
      throw new DomainException("该报工记录已作废，不能再作废");
    }
    this.voidedTime = moment;
  }

  /**
   * 是否已作废
   *
   * @return true 表示已作废
   */
  public boolean isVoided() {
    return this.voidedTime != null;
  }

  /**
   * 是否还算数（未作废）
   *
   * @return true 表示这笔报工有效
   */
  public boolean isEffective() {
    return this.voidedTime == null;
  }

  // ==================== 私有：校验与赋值 ====================

  /**
   * 统一的校验与赋值入口（构造器与修改共用）
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
   * @throws DomainException 工单/工序/工作中心为空、工时或数量为负、良品与报废同时为0时抛出
   */
  private void apply(
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
      @Nullable String remark) {
    if (workOrderId <= 0) {
      throw new DomainException("工单不能为空");
    }
    if (workOrderOperationId <= 0) {
      throw new DomainException("工单工序不能为空");
    }
    if (operationId <= 0) {
      throw new DomainException("工序不能为空");
    }
    if (workCenterId <= 0) {
      throw new DomainException("工作中心不能为空");
    }
    if (workTime.signum() < 0) {
      throw new DomainException("工时不能为负数");
    }
    if (goodQty.signum() < 0) {
      throw new DomainException("良品数量不能为负数");
    }
    if (lossQty.signum() < 0) {
      throw new DomainException("报废数量不能为负数");
    }
    if (goodQty.signum() == 0 && lossQty.signum() == 0) {
      throw new DomainException("良品数量与报废数量不能同时为0");
    }
    String trimmedWorkOrderNumber = StringUtils.trimToNull(workOrderNumber);
    String trimmedOperationName = StringUtils.trimToNull(operationName);
    String trimmedWorkCenterName = StringUtils.trimToNull(workCenterName);
    if (trimmedWorkOrderNumber == null) {
      throw new DomainException("工单号不能为空");
    }
    if (trimmedOperationName == null) {
      throw new DomainException("工序名称不能为空");
    }
    if (trimmedWorkCenterName == null) {
      throw new DomainException("工作中心名称不能为空");
    }

    this.workOrderId = workOrderId;
    this.workOrderNumber = trimmedWorkOrderNumber;
    this.workOrderOperationId = workOrderOperationId;
    this.operationId = operationId;
    this.operationName = trimmedOperationName;
    this.workCenterId = workCenterId;
    this.workCenterName = trimmedWorkCenterName;
    this.operatorId = operatorId;
    this.workTime = workTime;
    this.goodQty = goodQty;
    this.lossQty = lossQty;
    this.remark = remark;
  }
}
