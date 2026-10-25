package com.rongia.production.domain.workcalendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.rongia.shared.domain.exception.DomainException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 工作日历可用工时计算的纯算法测试 */
@DisplayName("工作日历 WorkCalendar")
class WorkCalendarTest {

  /** 建一张日历：周一到周五 8:00-12:00、13:00-17:00，周末不上班 */
  private static WorkCalendar weekdayCalendar() {
    WorkCalendar calendar = new WorkCalendar("STD", "标准工时", false, null);
    for (String day :
        new String[] {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"}) {
      calendar.addSlot(day, LocalTime.of(8, 0), LocalTime.of(12, 0));
      calendar.addSlot(day, LocalTime.of(13, 0), LocalTime.of(17, 0));
    }
    return calendar;
  }

  @Test
  @DisplayName("工作日一天 8 小时")
  void workdayIsEightHours() {
    WorkCalendar calendar = weekdayCalendar();
    // 2026-10-19 是周一
    assertEquals(480, calendar.availableMinutesOn(LocalDate.of(2026, 10, 19)));
  }

  @Test
  @DisplayName("周末没有班次，可用工时为 0")
  void weekendHasNoSlot() {
    WorkCalendar calendar = weekdayCalendar();
    // 2026-10-24 是周六
    assertEquals(0, calendar.availableMinutesOn(LocalDate.of(2026, 10, 24)));
  }

  @Test
  @DisplayName("整天例外把当天工时清零")
  void fullDayExceptionBlocksWholeDay() {
    WorkCalendar calendar = weekdayCalendar();
    calendar.addException(
        "国庆",
        LocalDateTime.of(2026, 10, 19, 0, 0),
        LocalDateTime.of(2026, 10, 20, 0, 0),
        null);

    assertEquals(0, calendar.availableMinutesOn(LocalDate.of(2026, 10, 19)));
    assertEquals(480, calendar.availableMinutesOn(LocalDate.of(2026, 10, 20)));
  }

  @Test
  @DisplayName("半天例外只扣重叠部分")
  void partialExceptionDeductsOverlap() {
    WorkCalendar calendar = weekdayCalendar();
    // 上午 8:00-10:00 检修
    calendar.addException(
        "检修",
        LocalDateTime.of(2026, 10, 19, 8, 0),
        LocalDateTime.of(2026, 10, 19, 10, 0),
        null);

    assertEquals(360, calendar.availableMinutesOn(LocalDate.of(2026, 10, 19)));
  }

  @Test
  @DisplayName("区间累加：一整周 5 天 × 8 小时")
  void weekTotal() {
    WorkCalendar calendar = weekdayCalendar();
    assertEquals(
        2400,
        calendar.availableMinutesBetween(
            LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 25)));
  }

  @Test
  @DisplayName("同星期时段重叠要拒绝")
  void overlappingSlotIsRejected() {
    WorkCalendar calendar = new WorkCalendar("STD", "标准工时", false, null);
    calendar.addSlot("Monday", LocalTime.of(8, 0), LocalTime.of(12, 0));

    assertThrows(
        DomainException.class,
        () -> calendar.addSlot("Monday", LocalTime.of(11, 0), LocalTime.of(13, 0)));
  }

  @Test
  @DisplayName("不同星期的相同时段不算重叠")
  void sameTimeOnDifferentDayIsAllowed() {
    WorkCalendar calendar = new WorkCalendar("STD", "标准工时", false, null);
    calendar.addSlot("Monday", LocalTime.of(8, 0), LocalTime.of(12, 0));
    calendar.addSlot("Tuesday", LocalTime.of(8, 0), LocalTime.of(12, 0));

    assertEquals(2, calendar.getSlots().size());
  }

  @Test
  @DisplayName("下班时间必须晚于上班时间")
  void invalidSlotRangeIsRejected() {
    WorkCalendar calendar = new WorkCalendar("STD", "标准工时", false, null);
    assertThrows(
        DomainException.class,
        () -> calendar.addSlot("Monday", LocalTime.of(12, 0), LocalTime.of(8, 0)));
  }

  @Test
  @DisplayName("结束日期早于开始日期要拒绝")
  void reversedRangeIsRejected() {
    WorkCalendar calendar = weekdayCalendar();
    assertThrows(
        DomainException.class,
        () ->
            calendar.availableMinutesBetween(
                LocalDate.of(2026, 10, 25), LocalDate.of(2026, 10, 19)));
  }
}
