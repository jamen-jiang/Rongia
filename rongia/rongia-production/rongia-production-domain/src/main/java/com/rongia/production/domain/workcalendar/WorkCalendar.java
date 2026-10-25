package com.rongia.production.domain.workcalendar;

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
 * <p>回答「哪几天上班、每天上几个钟」：常规时段按星期几排（{@link WorkCalendarSlot}），例外按日期区间
 * 覆盖（{@link WorkCalendarException}）。照 Odoo 19 的 {@code resource.calendar}，但不做两周一循环、
 * 不做按人差异——生产侧排的是机器，机器不会调休。
 *
 * <p>工作中心引用一张日历（{@code WorkCenter.workCalendarId}），排产时用它算「这段时间这台机器有多少
 * 可用工时」，再乘时间效率。没有日历就没有可用工时，所以工作中心必须先挂日历才能参与排产。
 *
 * <p>时区不存：全厂按同一个本地时区算，跨时区工厂各建各的日历即可。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "WorkCalendar",
    indexes = {
      @Index(name = "IX_WorkCalendar_Code", columnList = "Code"),
      @Index(name = "IX_WorkCalendar_Disabled", columnList = "Disabled")
      // 注意：Code 的过滤唯一索引带 WHERE IsDeleted=0，JPA 不支持，用 Flyway 创建
    })
public class WorkCalendar extends UpdateAggregateRoot {

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
  @JoinColumn(name = "WorkCalendarId")
  @SQLRestriction("IsDeleted = 0")
  private List<WorkCalendarSlot> slots = new ArrayList<>();

  /** 例外（节假日、检修、调休） */
  @OneToMany(
      fetch = FetchType.EAGER,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE},
      orphanRemoval = true)
  @JoinColumn(name = "WorkCalendarId")
  @SQLRestriction("IsDeleted = 0")
  private List<WorkCalendarException> exceptions = new ArrayList<>();

  /**
   * 获取常规工作时段（只读）
   *
   * @return 不可变列表
   */
  public List<WorkCalendarSlot> getSlots() {
    return Collections.unmodifiableList(slots);
  }

  /**
   * 获取例外（只读）
   *
   * @return 不可变列表
   */
  public List<WorkCalendarException> getExceptions() {
    return Collections.unmodifiableList(exceptions);
  }

  /**
   * 构造器
   *
   * @param code 代码
   * @param name 名称
   * @param disabled 是否停用
   * @param remark 备注（可空）
   */
  WorkCalendar(String code, String name, boolean disabled, @Nullable String remark) {
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
  public void addSlot(String dayOfWeek, LocalTime startTime, LocalTime endTime) {
    WorkCalendarSlot slot = new WorkCalendarSlot(dayOfWeek, startTime, endTime);
    if (isOverlapped(slot, null)) {
      throw new DomainException("该星期已有重叠的工作时段");
    }
    slots.add(slot);
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
  public void modifySlot(long id, String dayOfWeek, LocalTime startTime, LocalTime endTime) {
    WorkCalendarSlot target = findSlot(id);
    if (target == null) {
      throw new DomainException("工作时段不存在!");
    }
    WorkCalendarSlot candidate = new WorkCalendarSlot(dayOfWeek, startTime, endTime);
    if (isOverlapped(candidate, id)) {
      throw new DomainException("该星期已有重叠的工作时段");
    }
    target.modify(dayOfWeek, startTime, endTime);
  }

  /**
   * 删除常规时段（逻辑删除）
   *
   * @param id 时段Id
   */
  public void deleteSlot(long id) {
    WorkCalendarSlot target = findSlot(id);
    if (target != null) {
      target.delete();
    }
  }

  /**
   * 添加例外
   *
   * @param name 原因
   * @param startTime 开始时间
   * @param endTime 结束时间
   * @param remark 备注（可空）
   */
  public void addException(
      String name, LocalDateTime startTime, LocalDateTime endTime, @Nullable String remark) {
    exceptions.add(new WorkCalendarException(name, startTime, endTime, remark));
  }

  /**
   * 修改例外
   *
   * @param id 例外Id
   * @param name 原因
   * @param startTime 开始时间
   * @param endTime 结束时间
   * @param remark 备注（可空）
   * @throws DomainException 例外不存在时抛出
   */
  public void modifyException(
      long id,
      String name,
      LocalDateTime startTime,
      LocalDateTime endTime,
      @Nullable String remark) {
    WorkCalendarException target = findException(id);
    if (target == null) {
      throw new DomainException("日历例外不存在!");
    }
    target.modify(name, startTime, endTime, remark);
  }

  /**
   * 删除例外（逻辑删除）
   *
   * @param id 例外Id
   */
  public void deleteException(long id) {
    WorkCalendarException target = findException(id);
    if (target != null) {
      target.delete();
    }
  }

  /**
   * 指定日期上的可用工时（分钟）
   *
   * <p>先按星期取常规时段，再扣掉落在这一天里的例外时长。整天放假时返回 0；半天放假按区间重叠部分扣。
   * 例外跨出当天的部分不重复扣——只算与当天相交的那一段。
   *
   * @param date 日期
   * @return 可用分钟数（≥ 0）
   */
  public long availableMinutesOn(LocalDate date) {
    long total = 0;
    for (WorkCalendarSlot slot : slots) {
      if (slot.getIsDeleted() || !slot.isOn(date.getDayOfWeek())) {
        continue;
      }
      total += slot.minutes();
    }
    if (total == 0) {
      return 0;
    }

    LocalDateTime dayStart = date.atStartOfDay();
    LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
    long blocked = 0;
    for (WorkCalendarException exception : exceptions) {
      if (exception.getIsDeleted()) {
        continue;
      }
      LocalDateTime from =
          exception.getStartTime().isAfter(dayStart) ? exception.getStartTime() : dayStart;
      LocalDateTime to =
          exception.getEndTime().isBefore(dayEnd) ? exception.getEndTime() : dayEnd;
      if (to.isAfter(from)) {
        blocked += Duration.between(from, to).toMinutes();
      }
    }
    // 例外区间可能覆盖午休等非上班时段，按总时长扣会把休息时间也扣掉，这里只保证不为负
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
  private WorkCalendarSlot findSlot(long id) {
    return slots.stream()
        .filter(i -> java.util.Objects.equals(i.getId(), id) && !i.getIsDeleted())
        .findFirst()
        .orElse(null);
  }

  /**
   * 按Id查找未删除的例外
   *
   * @param id 例外Id
   * @return 匹配到的例外，找不到返回 {@code null}
   */
  @Nullable
  private WorkCalendarException findException(long id) {
    return exceptions.stream()
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
  private boolean isOverlapped(WorkCalendarSlot candidate, @Nullable Long excludeId) {
    return slots.stream()
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
