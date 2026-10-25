package com.rongia.production.domain.workcalendar;

import com.rongia.shared.domain.entity.BaseDeleteEntity;
import com.rongia.shared.domain.enums.DayOfWeekEnum;
import com.rongia.shared.domain.exception.DomainException;
import com.rongia.shared.domain.util.EnumUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;
import org.jspecify.annotations.NullMarked;

/**
 * 工作日时段（日历的子行）
 *
 * <p>一行 = 某个星期几的一段上班时间，照 Odoo 19 的 {@code resource.calendar.attendance}。
 * 一天可以有多段（上午 8:00–12:00、下午 13:00–17:00），午休不排进来就是两行，不需要单独的「休息」类型。
 *
 * <p>时段不能跨零点：夜班 22:00 到次日 6:00 要拆成两行（当晚 22:00–24:00、次日 0:00–6:00），
 * 否则「这一天上了多少工时」就没法按自然日归集。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "WorkCalendarSlot",
    indexes = {
      @Index(name = "IX_WorkCalendarSlot_WorkCalendarId", columnList = "WorkCalendarId"),
      @Index(name = "IX_WorkCalendarSlot_DayOfWeek", columnList = "DayOfWeek")
    })
public class WorkCalendarSlot extends BaseDeleteEntity {

  /** 工作日历Id */
  @Column(name = "WorkCalendarId")
  private Long workCalendarId;

  /** 星期几，取值见 {@link DayOfWeekEnum} */
  @Nationalized
  @Column(name = "DayOfWeek", nullable = false, length = 20)
  private String dayOfWeek;

  /** 上班时间（含） */
  @Column(name = "StartTime", nullable = false)
  private LocalTime startTime;

  /** 下班时间（不含） */
  @Column(name = "EndTime", nullable = false)
  private LocalTime endTime;

  /**
   * 构造器
   *
   * @param dayOfWeek 星期几编码
   * @param startTime 上班时间
   * @param endTime 下班时间
   * @throws DomainException 星期编码非法、或下班时间不晚于上班时间时抛出
   */
  WorkCalendarSlot(String dayOfWeek, LocalTime startTime, LocalTime endTime) {
    apply(dayOfWeek, startTime, endTime);
  }

  /**
   * 修改时段
   *
   * @param dayOfWeek 星期几编码
   * @param startTime 上班时间
   * @param endTime 下班时间
   * @throws DomainException 星期编码非法、或下班时间不晚于上班时间时抛出
   */
  void modify(String dayOfWeek, LocalTime startTime, LocalTime endTime) {
    apply(dayOfWeek, startTime, endTime);
  }

  /**
   * 这个时段的时长（分钟）
   *
   * @return 分钟数
   */
  public long minutes() {
    return Duration.between(startTime, endTime).toMinutes();
  }

  /**
   * 是否落在指定星期
   *
   * @param day 要判断的星期
   * @return true：这个时段属于那一天
   */
  public boolean isOn(DayOfWeek day) {
    return EnumUtils.fromCode(DayOfWeekEnum.class, dayOfWeek).toDayOfWeek() == day;
  }

  /**
   * 统一的校验与赋值入口（构造器与修改共用）
   *
   * @param dayOfWeek 星期几编码
   * @param startTime 上班时间
   * @param endTime 下班时间
   * @throws DomainException 星期编码非法、或下班时间不晚于上班时间时抛出
   */
  private void apply(String dayOfWeek, LocalTime startTime, LocalTime endTime) {
    this.dayOfWeek = EnumUtils.getCode(DayOfWeekEnum.class, dayOfWeek, "星期");
    if (startTime == null || endTime == null) {
      throw new DomainException("上下班时间不能为空");
    }
    if (!endTime.isAfter(startTime)) {
      throw new DomainException("下班时间必须晚于上班时间");
    }

    this.startTime = startTime;
    this.endTime = endTime;
  }
}
