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
 * <p>排程与成本归集的单位，也是工序挂靠的对象：一条镀膜线、一个抛光班组各是一个工作中心。 中心只回答「这条线有多快、多少钱一小时、默认从哪发料收货」——一天能干多少由排产按工作日历 ×
 * 效率算，不在这里存。 同一条线做不同物料的单价差异也不在这里，见 {@code RoutingOperation} 引用的费率卡。
 *
 * <p>准备时间与清理时间挂在这里当默认值：换型调试、完工清场是这条线固有的开销，不随物料变。 个别工序若另有口径，在路线工序上单独指定。
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

  /** 工作日历Id（照 Odoo 的 resource_calendar_id）：排产用它算这条线一天有多少可用工时 */
  @Column(name = "WorkCalendarId", nullable = false)
  private Long workCalendarId;

  /** 效率（百分比，100 = 按标准工时满负荷产出，照 Odoo 的 time_efficiency） */
  @Column(name = "Efficiency", nullable = false, precision = 5, scale = 2)
  private BigDecimal efficiency;

  /** 小时费率（按币种，照 Odoo 的 costs_hour）：这条线开一小时的成本 */
  @Column(name = "HourRate", nullable = false, precision = 18, scale = 2)
  private BigDecimal hourRate;

  /** 小时费率的币种Id（照 Odoo 的 currency_id，Odoo 从公司带出，我们没有公司维度所以单存） */
  @Column(name = "CurrencyId", nullable = false)
  private Long currencyId;

  /** 准备时间（分钟，照 Odoo 的 time_start）：换型调试的固定开销 */
  @Column(name = "SetupTime", nullable = false, precision = 18, scale = 2)
  private BigDecimal setupTime;

  /** 清理时间（分钟，照 Odoo 的 time_stop）：完工后清场的固定开销 */
  @Column(name = "CleanupTime", nullable = false, precision = 18, scale = 2)
  private BigDecimal cleanupTime;

  /** 默认产能（这台中心同时能容的工位数/人数，照 Odoo 的 default_capacity） */
  @Column(name = "DefaultCapacity", nullable = false)
  private Integer defaultCapacity;

  /** 默认发料仓库Id（可空，照 Odoo 的 default_location_src_id：挂这条线的工序领料默认从这发） */
  @Column(name = "SourceWarehouseId")
  @Nullable
  private Long sourceWarehouseId;

  /** 默认收货仓库Id（可空，照 Odoo 的 default_location_dest_id：这道工序的产出默认进这） */
  @Column(name = "DestWarehouseId")
  @Nullable
  private Long destWarehouseId;

  /** 实时状态编码，取值见 {@link WorkCenterWorkingStateEnum}（车间现场改，排产只读） */
  @Nationalized
  @Column(name = "WorkingState", nullable = false, length = 50)
  private String workingState;

  /** 是否停用 */
  @Column(name = "Disabled", nullable = false)
  private Boolean disabled;

  /** 排序（列表展示顺序，照 Odoo 的 sequence） */
  @Column(name = "Sequence", nullable = false)
  private Integer sequence;

  /** 备注（可空） */
  @Nationalized
  @Column(name = "Remark", length = 500)
  @Nullable
  private String remark;

  /**
   * 构造器
   *
   * @param name 名称
   * @param workCalendarId 工作日历Id
   * @param efficiency 效率（百分比）
   * @param hourRate 小时费率
   * @param currencyId 小时费率的币种Id
   * @param setupTime 准备时间（分钟）
   * @param cleanupTime 清理时间（分钟）
   * @param defaultCapacity 默认产能
   * @param sourceWarehouseId 默认发料仓库Id（可空）
   * @param destWarehouseId 默认收货仓库Id（可空）
   * @param workingState 实时状态编码
   * @param disabled 是否停用
   * @param sequence 排序
   * @param remark 备注（可空）
   */
  WorkCenter(
      String name,
      long workCalendarId,
      BigDecimal efficiency,
      BigDecimal hourRate,
      long currencyId,
      BigDecimal setupTime,
      BigDecimal cleanupTime,
      int defaultCapacity,
      @Nullable Long sourceWarehouseId,
      @Nullable Long destWarehouseId,
      String workingState,
      boolean disabled,
      int sequence,
      @Nullable String remark) {
    apply(
        name,
        workCalendarId,
        efficiency,
        hourRate,
        currencyId,
        setupTime,
        cleanupTime,
        defaultCapacity,
        sourceWarehouseId,
        destWarehouseId,
        workingState,
        disabled,
        sequence,
        remark);
  }

  /**
   * 修改工作中心
   *
   * @param name 名称
   * @param workCalendarId 工作日历Id
   * @param efficiency 效率（百分比）
   * @param hourRate 小时费率
   * @param currencyId 小时费率的币种Id
   * @param setupTime 准备时间（分钟）
   * @param cleanupTime 清理时间（分钟）
   * @param defaultCapacity 默认产能
   * @param sourceWarehouseId 默认发料仓库Id（可空）
   * @param destWarehouseId 默认收货仓库Id（可空）
   * @param workingState 实时状态编码
   * @param disabled 是否停用
   * @param sequence 排序
   * @param remark 备注（可空）
   */
  void modify(
      String name,
      long workCalendarId,
      BigDecimal efficiency,
      BigDecimal hourRate,
      long currencyId,
      BigDecimal setupTime,
      BigDecimal cleanupTime,
      int defaultCapacity,
      @Nullable Long sourceWarehouseId,
      @Nullable Long destWarehouseId,
      String workingState,
      boolean disabled,
      int sequence,
      @Nullable String remark) {
    apply(
        name,
        workCalendarId,
        efficiency,
        hourRate,
        currencyId,
        setupTime,
        cleanupTime,
        defaultCapacity,
        sourceWarehouseId,
        destWarehouseId,
        workingState,
        disabled,
        sequence,
        remark);
  }

  // ==================== 私有：校验与赋值 ====================

  /**
   * 统一的校验与赋值入口（构造器与修改共用）
   *
   * <p>把「去空格 → 非空校验 → 数值非负校验 → 赋值」收敛到一处，避免构造器和 {@link #modify} 各写一遍导致规则漂移。
   *
   * @param name 名称
   * @param workCalendarId 工作日历Id
   * @param efficiency 效率（百分比）
   * @param hourRate 小时费率
   * @param currencyId 小时费率的币种Id
   * @param setupTime 准备时间（分钟）
   * @param cleanupTime 清理时间（分钟）
   * @param defaultCapacity 默认产能
   * @param sourceWarehouseId 默认发料仓库Id（可空）
   * @param destWarehouseId 默认收货仓库Id（可空）
   * @param workingState 实时状态编码
   * @param disabled 是否停用
   * @param sequence 排序
   * @param remark 备注（可空）
   * @throws DomainException 名称为空、产能非正、仓库Id非法、排序非正、效率非正、费率或工时为负、日历为空时抛出
   */
  private void apply(
      String name,
      long workCalendarId,
      BigDecimal efficiency,
      BigDecimal hourRate,
      long currencyId,
      BigDecimal setupTime,
      BigDecimal cleanupTime,
      int defaultCapacity,
      @Nullable Long sourceWarehouseId,
      @Nullable Long destWarehouseId,
      String workingState,
      boolean disabled,
      int sequence,
      @Nullable String remark) {
    String trimmedName = StringUtils.trimToNull(name);
    if (trimmedName == null) {
      throw new DomainException("工作中心名称不能为空");
    }
    String trimmedWorkingState = StringUtils.trimToNull(workingState);
    if (trimmedWorkingState == null) {
      throw new DomainException("实时状态不能为空");
    }
    String validWorkingState = EnumUtils.getCode(WorkCenterWorkingStateEnum.class, trimmedWorkingState, "实时状态");
    if (defaultCapacity <= 0) {
      throw new DomainException("默认产能必须大于0");
    }
    if (sourceWarehouseId != null && sourceWarehouseId <= 0) {
      throw new DomainException("默认发料仓库Id有误!");
    }
    if (destWarehouseId != null && destWarehouseId <= 0) {
      throw new DomainException("默认收货仓库Id有误!");
    }
    if (sequence <= 0) {
      throw new DomainException("排序必须大于0");
    }
    if (efficiency.signum() <= 0) {
      throw new DomainException("效率必须大于0");
    }
    if (hourRate.signum() < 0) {
      throw new DomainException("小时费率不能为负数");
    }
    if (currencyId <= 0) {
      throw new DomainException("小时费率的币种不能为空");
    }
    if (setupTime.signum() < 0) {
      throw new DomainException("准备时间不能为负数");
    }
    if (cleanupTime.signum() < 0) {
      throw new DomainException("清理时间不能为负数");
    }
    if (workCalendarId <= 0) {
      throw new DomainException("工作日历不能为空");
    }

    this.name = trimmedName;
    this.workCalendarId = workCalendarId;
    this.efficiency = efficiency;
    this.hourRate = hourRate;
    this.currencyId = currencyId;
    this.setupTime = setupTime;
    this.cleanupTime = cleanupTime;
    this.defaultCapacity = defaultCapacity;
    this.sourceWarehouseId = sourceWarehouseId;
    this.destWarehouseId = destWarehouseId;
    this.workingState = validWorkingState;
    this.disabled = disabled;
    this.sequence = sequence;
    this.remark = remark;
  }
}
