package com.rongia.production.domain.calendar;

import com.rongia.shared.domain.entity.BaseDeleteEntity;
import com.rongia.shared.domain.exception.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
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
 * 日历请假（日历的子行）
 *
 * <p>照 Odoo 19 的 {@code resource.calendar.leaves}：在一段起止时间内不按常规时段上班。
 * 法定节假日（整天不上）和临时停工（某几个小时不上）都用它表达。
 *
 * <p>只存时间区间，不存「放几天」：跨天假期写成起止两个时点，算可用工时时按时段逐日比对，
 * 不必在假期上再维护一份日期清单。
 *
 * <p>它只减不增：要表达「周末加班」这种额外上班，在常规时段上加一行对应的星期与时段即可，
 * 不在这里用负的请假去抵消。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "CalendarLeave",
    indexes = {
      @Index(name = "IX_CalendarLeave_CalendarId", columnList = "CalendarId"),
      @Index(name = "IX_CalendarLeave_StartTime", columnList = "StartTime")
    })
public class CalendarLeave extends BaseDeleteEntity {

  /** 工作日历Id */
  @Column(name = "CalendarId")
  private Long calendarId;

  /** 原因（如「国庆」「设备检修」） */
  @Nationalized
  @Column(name = "Name", nullable = false, length = 200)
  private String name;

  /** 开始时间（含） */
  @Column(name = "StartTime", nullable = false)
  private LocalDateTime startTime;

  /** 结束时间（不含） */
  @Column(name = "EndTime", nullable = false)
  private LocalDateTime endTime;

  /** 备注（可空） */
  @Nationalized
  @Column(name = "Remark", length = 500)
  @Nullable
  private String remark;

  /**
   * 构造器
   *
   * @param name 原因
   * @param startTime 开始时间
   * @param endTime 结束时间
   * @param remark 备注（可空）
   * @throws DomainException 原因为空、或结束时间不晚于开始时间时抛出
   */
  CalendarLeave(
      String name, LocalDateTime startTime, LocalDateTime endTime, @Nullable String remark) {
    apply(name, startTime, endTime, remark);
  }

  /**
   * 修改请假
   *
   * @param name 原因
   * @param startTime 开始时间
   * @param endTime 结束时间
   * @param remark 备注（可空）
   * @throws DomainException 原因为空、或结束时间不晚于开始时间时抛出
   */
  void modify(
      String name, LocalDateTime startTime, LocalDateTime endTime, @Nullable String remark) {
    apply(name, startTime, endTime, remark);
  }

  /**
   * 是否覆盖指定时点
   *
   * @param moment 要判断的时点
   * @return true：这个时点落在请假区间内
   */
  public boolean covers(LocalDateTime moment) {
    return !moment.isBefore(startTime) && moment.isBefore(endTime);
  }

  /**
   * 统一的校验与赋值入口（构造器与修改共用）
   *
   * @param name 原因
   * @param startTime 开始时间
   * @param endTime 结束时间
   * @param remark 备注（可空）
   * @throws DomainException 原因为空、或结束时间不晚于开始时间时抛出
   */
  private void apply(
      String name, LocalDateTime startTime, LocalDateTime endTime, @Nullable String remark) {
    String trimmedName = StringUtils.trimToNull(name);
    if (trimmedName == null) {
      throw new DomainException("请假原因不能为空");
    }
    if (startTime == null || endTime == null) {
      throw new DomainException("请假的起止时间不能为空");
    }
    if (!endTime.isAfter(startTime)) {
      throw new DomainException("请假的结束时间必须晚于开始时间");
    }

    this.name = trimmedName;
    this.startTime = startTime;
    this.endTime = endTime;
    this.remark = remark;
  }
}
