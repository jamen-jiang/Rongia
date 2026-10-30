package com.rongia.production.domain.workcenter;

import com.rongia.shared.domain.aggregate.UpdateAggregateRoot;
import com.rongia.shared.domain.exception.DomainException;
import com.rongia.shared.domain.util.EnumUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Nationalized;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 工作中心
 *
 * <p>排程与成本归集的单位，也是工序挂靠的对象：一条镀膜线、一个抛光班组各是一个工作中心，都按工作日历算可用工时。
 *
 * <p>中心只回答「这条线开一小时三类成本各是多少」——人工、机器、制造费用三个费率分开记，
 * 自动化程度不同的线成本结构才拆得开；能干多少、能同时开几张单，由排产按工作日历和工序时长算，不在这里存。
 *
 * <p>换型调试、完工清场的开销不在这里：它取决于「上一个物料 → 这个物料」，不是这条线固有的数。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "WorkCenter",
    indexes = {
      @Index(name = "IX_WorkCenter_Name", columnList = "Name"),
      @Index(name = "IX_WorkCenter_Disabled", columnList = "Disabled")
    })
public class WorkCenter extends UpdateAggregateRoot {

  /** 名称 */
  @Nationalized
  @Column(name = "Name", nullable = false, length = 200)
  private String name;

  /** 日历Id（照 Odoo 的 resource_calendar_id）：排产用它算这条线一天有多少可用工时 */
  @Column(name = "CalendarId", nullable = false)
  private Long calendarId;

  /** 人工费率（元/小时，照 carbon 的 laborRate）：干活的人开一小时多少钱 */
  @Column(name = "LaborRate", nullable = false, precision = 18, scale = 2)
  private BigDecimal laborRate;

  /** 机器费率（元/小时，照 carbon 的 machineRate）：这台机转一小时多少钱（折旧、电、维护） */
  @Column(name = "MachineRate", nullable = false, precision = 18, scale = 2)
  private BigDecimal machineRate;

  /** 制造费用率（元/小时，照 carbon 的 overheadRate）：厂房、管理这些公共开销按小时摊进来的份额 */
  @Column(name = "OverheadRate", nullable = false, precision = 18, scale = 2)
  private BigDecimal overheadRate;

  /** 一批最多多少件（可空，照 carbon 的 batchCapacity）：镀膜一炉 200 片；空表示这台机不分批 */
  @Column(name = "BatchCapacity", precision = 18, scale = 2)
  @Nullable
  private BigDecimal batchCapacity;

  /** 状态编码，取值见 {@link WorkCenterStatusEnum}（照 ERPNext 的 Workstation.status，车间现场改，排产只读） */
  @Nationalized
  @Column(name = "Status", nullable = false, length = 50)
  private String status;

  /** 是否停用（照 ERPNext 的 Workstation.disabled）：停用的中心不能再派新工单 */
  @Column(name = "Disabled", nullable = false)
  private Boolean disabled;

  /** 备注（可空） */
  @Nationalized
  @Column(name = "Remark", length = 500)
  @Nullable
  private String remark;

  /**
   * 构造器
   *
   * @param name 名称
   * @param calendarId 日历Id
   * @param laborRate 人工费率
   * @param machineRate 机器费率
   * @param overheadRate 制造费用率
   * @param batchCapacity 一批最多多少件（可空）
   * @param status 状态编码
   * @param disabled 是否停用
   * @param remark 备注（可空）
   */
  WorkCenter(
      String name,
      long calendarId,
      BigDecimal laborRate,
      BigDecimal machineRate,
      BigDecimal overheadRate,
      @Nullable BigDecimal batchCapacity,
      String status,
      boolean disabled,
      @Nullable String remark) {
    apply(
        name,
        calendarId,
        laborRate,
        machineRate,
        overheadRate,
        batchCapacity,
        status,
        disabled,
        remark);
  }

  /**
   * 修改工作中心
   *
   * @param name 名称
   * @param calendarId 日历Id
   * @param laborRate 人工费率
   * @param machineRate 机器费率
   * @param overheadRate 制造费用率
   * @param batchCapacity 一批最多多少件（可空）
   * @param status 状态编码
   * @param disabled 是否停用
   * @param remark 备注（可空）
   */
  void modify(
      String name,
      long calendarId,
      BigDecimal laborRate,
      BigDecimal machineRate,
      BigDecimal overheadRate,
      @Nullable BigDecimal batchCapacity,
      String status,
      boolean disabled,
      @Nullable String remark) {
    apply(
        name,
        calendarId,
        laborRate,
        machineRate,
        overheadRate,
        batchCapacity,
        status,
        disabled,
        remark);
  }

  // ==================== 私有：校验与赋值 ====================

  /**
   * 统一的校验与赋值入口（构造器与修改共用）
   *
   * <p>把「去空格 → 非空校验 → 数值非负校验 → 赋值」收敛到一处，避免构造器和 {@link #modify} 各写一遍导致规则漂移。
   *
   * @param name 名称
   * @param calendarId 日历Id
   * @param laborRate 人工费率
   * @param machineRate 机器费率
   * @param overheadRate 制造费用率
   * @param batchCapacity 一批最多多少件（可空）
   * @param status 状态编码
   * @param disabled 是否停用
   * @param remark 备注（可空）
   * @throws DomainException 名称为空、状态为空或非法、日历为空、任一费率或批量件数为负数时抛出
   */
  private void apply(
      String name,
      long calendarId,
      BigDecimal laborRate,
      BigDecimal machineRate,
      BigDecimal overheadRate,
      @Nullable BigDecimal batchCapacity,
      String status,
      boolean disabled,
      @Nullable String remark) {
    String trimmedName = StringUtils.trimToNull(name);
    if (trimmedName == null) {
      throw new DomainException("工作中心名称不能为空");
    }
    String trimmedStatus = StringUtils.trimToNull(status);
    if (trimmedStatus == null) {
      throw new DomainException("状态不能为空");
    }
    String validStatus = EnumUtils.getCode(WorkCenterStatusEnum.class, trimmedStatus, "状态");
    if (calendarId <= 0) {
      throw new DomainException("工作日历不能为空");
    }
    if (laborRate.signum() < 0) {
      throw new DomainException("人工费率不能为负数");
    }
    if (machineRate.signum() < 0) {
      throw new DomainException("机器费率不能为负数");
    }
    if (overheadRate.signum() < 0) {
      throw new DomainException("制造费用率不能为负数");
    }
    if (batchCapacity != null && batchCapacity.signum() < 0) {
      throw new DomainException("一批最多件数不能为负数");
    }

    this.name = trimmedName;
    this.calendarId = calendarId;
    this.laborRate = laborRate;
    this.machineRate = machineRate;
    this.overheadRate = overheadRate;
    this.batchCapacity = batchCapacity;
    this.status = validStatus;
    this.disabled = disabled;
    this.remark = remark;
  }
}
