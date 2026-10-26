package com.rongia.production.domain.calendar;

import com.rongia.shared.domain.aggregate.UpdateAggregateRoot;
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
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
 * 工作日历
 *
 * <p>回答「哪几天上班、每天上几个钟」：常规时段按星期几排（{@link CalendarAttendance}），请假按日期区间
 * 覆盖（{@link CalendarLeave}）。照 Odoo 19 的 {@code resource.calendar}，但不做两周一循环、
 * 不做按人差异——生产侧排的是机器，机器不会调休。
 *
 * <p>工作中心引用一张日历（{@code WorkCenter.calendarId}），排产时用它算「这段时间这台机器有多少
 * 可用工时」。没有日历就没有可用工时，所以工作中心必须先挂日历才能参与排产。
 *
 * <p>时区不存：全厂按同一个本地时区算，跨时区工厂各建各的日历即可。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "Calendar",
    indexes = {
      @Index(name = "IX_Calendar_Code", columnList = "Code"),
      @Index(name = "IX_Calendar_Disabled", columnList = "Disabled")
      // 注意：Code 的过滤唯一索引带 WHERE IsDeleted=0，JPA 不支持，用 Flyway 创建
    })
public class Calendar extends UpdateAggregateRoot {

  /** 代码 */
  @Nationalized
  @Column(name = "Code", nullable = false, length = 100)
  private String code;

  /** 名称 */
  @Nationalized
  @Column(name = "Name", nullable = false, length = 200)
  private String name;

  /** 是否停用：停用的日历不能再挂到新的工作中心上 */
  @Column(name = "Disabled", nullable = false)
  private Boolean disabled = false;

  /** 备注（可空） */
  @Nationalized
  @Column(name = "Remark", length = 500)
  @Nullable
  private String remark;

  /** 常规工作时段 */
  @OneToMany(
      fetch = FetchType.EAGER,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE},
      orphanRemoval = true)
  @JoinColumn(name = "CalendarId")
  @SQLRestriction("IsDeleted = 0")
  private List<CalendarAttendance> attendances = new ArrayList<>();

  /** 请假（节假日、检修、调休） */
  @OneToMany(
      fetch = FetchType.EAGER,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE},
      orphanRemoval = true)
  @JoinColumn(name = "CalendarId")
  @SQLRestriction("IsDeleted = 0")
  private List<CalendarLeave> leaves = new ArrayList<>();

  /**
   * 获取常规工作时段（只读）
   *
   * @return 不可变列表
   */
  public List<CalendarAttendance> getAttendances() {
    return Collections.unmodifiableList(attendances);
  }

  /**
   * 获取请假（只读）
   *
   * @return 不可变列表
   */
  public List<CalendarLeave> getLeaves() {
    return Collections.unmodifiableList(leaves);
  }

  /**
   * 构造器
   *
   * @param code 代码
   * @param name 名称
   * @param disabled 是否停用
   * @param remark 备注（可空）
   */
  Calendar(String code, String name, boolean disabled, @Nullable String remark) {
    apply(code, name, disabled, remark);
  }

  /**
   * 修改日历
   *
   * @param code 代码
   * @param name 名称
   * @param disabled 是否停用
   * @param remark 备注（可空）
   */
  void modify(String code, String name, boolean disabled, @Nullable String remark) {
    apply(code, name, disabled, remark);
  }

  /**
   * 添加常规时段
   *
   * @param dayOfWeek 星期几编码
   * @param startTime 上班时间
   * @param endTime 下班时间
   * @throws DomainException 与已有时段重叠时抛出
   */
  public void addAttendance(String dayOfWeek, LocalTime startTime, LocalTime endTime) {
    CalendarAttendance attendance = new CalendarAttendance(dayOfWeek, startTime, endTime);
    if (isAttendanceOverlapped(attendance, null)) {
      throw new DomainException("该星期已有重叠的工作时段");
    }
    attendances.add(attendance);
  }

  /**
   * 修改常规时段
   *
   * @param id 时段Id
   * @param dayOfWeek 星期几编码
   * @param startTime 上班时间
   * @param endTime 下班时间
   * @throws DomainException 时段不存在、或与其它时段重叠时抛出
   */
  public void modifyAttendance(long id, String dayOfWeek, LocalTime startTime, LocalTime endTime) {
    CalendarAttendance target = findAttendance(id);
    if (target == null) {
      throw new DomainException("工作时段不存在!");
    }
    CalendarAttendance candidate = new CalendarAttendance(dayOfWeek, startTime, endTime);
    if (isAttendanceOverlapped(candidate, id)) {
      throw new DomainException("该星期已有重叠的工作时段");
    }
    target.modify(dayOfWeek, startTime, endTime);
  }

  /**
   * 删除常规时段（逻辑删除）
   *
   * @param id 时段Id
   */
  public void deleteAttendance(long id) {
    CalendarAttendance target = findAttendance(id);
    if (target != null) {
      target.delete();
    }
  }

  /**
   * 添加请假
   *
   * @param name 原因
   * @param startTime 开始时间
   * @param endTime 结束时间
   * @param remark 备注（可空）
   */
  public void addLeave(
      String name, LocalDateTime startTime, LocalDateTime endTime, @Nullable String remark) {
    leaves.add(new CalendarLeave(name, startTime, endTime, remark));
  }

  /**
   * 修改请假
   *
   * @param id 请假Id
   * @param name 原因
   * @param startTime 开始时间
   * @param endTime 结束时间
   * @param remark 备注（可空）
   * @throws DomainException 请假不存在时抛出
   */
  public void modifyLeave(
      long id,
      String name,
      LocalDateTime startTime,
      LocalDateTime endTime,
      @Nullable String remark) {
    CalendarLeave target = findLeave(id);
    if (target == null) {
      throw new DomainException("日历请假不存在!");
    }
    target.modify(name, startTime, endTime, remark);
  }

  /**
   * 删除请假（逻辑删除）
   *
   * @param id 请假Id
   */
  public void deleteLeave(long id) {
    CalendarLeave target = findLeave(id);
    if (target != null) {
      target.delete();
    }
  }

  /**
   * 指定日期上的可用工时（分钟）
   *
   * <p>先按星期取常规时段，再扣掉落在这一天里的请假时长。整天放假时返回 0；半天放假按区间重叠部分扣。
   * 请假跨出当天的部分不重复扣——只算与当天相交的那一段。
   *
   * @param date 日期
   * @return 可用分钟数（≥ 0）
   */
  public long availableMinutesOn(LocalDate date) {
    long total = 0;
    for (CalendarAttendance attendance : attendances) {
      if (attendance.getIsDeleted() || !attendance.isOn(date.getDayOfWeek())) {
        continue;
      }
      total += attendance.minutes();
    }
    if (total == 0) {
      return 0;
    }

    LocalDateTime dayStart = date.atStartOfDay();
    LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
    long blocked = 0;
    for (CalendarLeave leave : leaves) {
      if (leave.getIsDeleted()) {
        continue;
      }
      LocalDateTime from =
          leave.getStartTime().isAfter(dayStart) ? leave.getStartTime() : dayStart;
      LocalDateTime to =
          leave.getEndTime().isBefore(dayEnd) ? leave.getEndTime() : dayEnd;
      if (to.isAfter(from)) {
        blocked += Duration.between(from, to).toMinutes();
      }
    }
    // 请假区间可能覆盖午休等非上班时段，按总时长扣会把休息时间也扣掉，这里只保证不为负
    return Math.max(0, total - blocked);
  }

  /**
   * 区间内的可用工时（分钟）
   *
   * <p>逐日累加 {@link #availableMinutesOn(LocalDate)}。首尾不按小时截断——排产按天排，
   * 半天开工的场景由调用方自己折算。
   *
   * @param from 开始日期（含）
   * @param to 结束日期（含）
   * @return 可用分钟数
   * @throws DomainException 结束日期早于开始日期时抛出
   */
  public long availableMinutesBetween(LocalDate from, LocalDate to) {
    if (to.isBefore(from)) {
      throw new DomainException("结束日期不能早于开始日期");
    }
    long total = 0;
    for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
      total += availableMinutesOn(date);
    }
    return total;
  }

  // ==================== 私有 ====================

  /**
   * 按Id查找未删除的时段
   *
   * @param id 时段Id
   * @return 匹配到的时段，找不到返回 {@code null}
   */
  @Nullable
  private CalendarAttendance findAttendance(long id) {
    return attendances.stream()
        .filter(i -> java.util.Objects.equals(i.getId(), id) && !i.getIsDeleted())
        .findFirst()
        .orElse(null);
  }

  /**
   * 按Id查找未删除的请假
   *
   * @param id 请假Id
   * @return 匹配到的请假，找不到返回 {@code null}
   */
  @Nullable
  private CalendarLeave findLeave(long id) {
    return leaves.stream()
        .filter(i -> java.util.Objects.equals(i.getId(), id) && !i.getIsDeleted())
        .findFirst()
        .orElse(null);
  }

  /**
   * 判断时段是否与同星期的其它时段重叠
   *
   * @param candidate 待校验的时段
   * @param excludeId 需要排除的时段Id（新增时传 {@code null}）
   * @return true：有重叠
   */
  private boolean isAttendanceOverlapped(CalendarAttendance candidate, @Nullable Long excludeId) {
    return attendances.stream()
        .filter(i -> !i.getIsDeleted())
        .filter(i -> !java.util.Objects.equals(i.getId(), excludeId))
        .filter(i -> i.getDayOfWeek().equals(candidate.getDayOfWeek()))
        .anyMatch(
            i ->
                candidate.getStartTime().isBefore(i.getEndTime())
                    && i.getStartTime().isBefore(candidate.getEndTime()));
  }

  /**
   * 统一的校验与赋值入口（构造器与修改共用）
   *
   * @param code 代码
   * @param name 名称
   * @param disabled 是否停用
   * @param remark 备注（可空）
   * @throws DomainException 代码或名称为空时抛出
   */
  private void apply(String code, String name, boolean disabled, @Nullable String remark) {
    String trimmedCode = StringUtils.trimToNull(code);
    String trimmedName = StringUtils.trimToNull(name);
    if (trimmedCode == null) {
      throw new DomainException("工作日历代码不能为空");
    }
    if (trimmedName == null) {
      throw new DomainException("工作日历名称不能为空");
    }
    this.code = trimmedCode;
    this.name = trimmedName;
    this.disabled = disabled;
    this.remark = remark;
  }
}
