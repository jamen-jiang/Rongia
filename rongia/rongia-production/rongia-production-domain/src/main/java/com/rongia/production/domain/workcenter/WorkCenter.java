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
 * <p>中心只回答「这条线有多快、同时能开几张单、开一小时多少钱」——一天能干多少由排产按工作日历算，不在这里存。
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

  /** 并行接单数：这个中心同时能开几张工单（照 ERPNext 的 production_capacity） */
  @Column(name = "ProductionCapacity", nullable = false)
  private Integer productionCapacity;

  /** 小时费率（照 Odoo 的 costs_hour）：这条线开一小时的成本 */
  @Column(name = "HourlyRate", nullable = false, precision = 18, scale = 2)
  private BigDecimal hourlyRate;

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
   * @param productionCapacity 并行接单数
   * @param hourlyRate 小时费率
   * @param status 状态编码
   * @param disabled 是否停用
   * @param remark 备注（可空）
   */
  WorkCenter(
      String name,
      long calendarId,
      int productionCapacity,
      BigDecimal hourlyRate,
      String status,
      boolean disabled,
      @Nullable String remark) {
    apply(
        name,
        calendarId,
        productionCapacity,
        hourlyRate,
        status,
        disabled,
        remark);
  }

  /**
   * 修改工作中心
   *
   * @param name 名称
   * @param calendarId 日历Id
   * @param productionCapacity 并行接单数
   * @param hourlyRate 小时费率
   * @param status 状态编码
   * @param disabled 是否停用
   * @param remark 备注（可空）
   */
  void modify(
      String name,
      long calendarId,
      int productionCapacity,
      BigDecimal hourlyRate,
      String status,
      boolean disabled,
      @Nullable String remark) {
    apply(
        name,
        calendarId,
        productionCapacity,
        hourlyRate,
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
   * @param productionCapacity 并行接单数
   * @param hourlyRate 小时费率
   * @param status 状态编码
   * @param disabled 是否停用
   * @param remark 备注（可空）
   * @throws DomainException 名称为空、状态为空或非法、日历为空、并行接单数非正、费率负数时抛出
   */
  private void apply(
      String name,
      long calendarId,
      int productionCapacity,
      BigDecimal hourlyRate,
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
    if (productionCapacity <= 0) {
      throw new DomainException("并行接单数必须大于0");
    }
    if (hourlyRate.signum() < 0) {
      throw new DomainException("小时费率不能为负数");
    }

    this.name = trimmedName;
    this.calendarId = calendarId;
    this.productionCapacity = productionCapacity;
    this.hourlyRate = hourlyRate;
    this.status = validStatus;
    this.disabled = disabled;
    this.remark = remark;
  }
}
