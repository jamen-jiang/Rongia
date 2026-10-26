package com.rongia.production.domain.calendar;

import com.rongia.shared.domain.exception.DomainException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 工作日历领域服务，负责跨聚合编排与子行同步 */
@NullMarked
public class CalendarDomainService {

  /** 工作日历工厂 */
  private final CalendarFactory calendarFactory;

  /** 工作日历仓储 */
  private final CalendarRepository calendarRepository;

  /**
   * 构造器注入
   *
   * @param calendarFactory 工作日历工厂
   * @param calendarRepository 工作日历仓储
   */
  public CalendarDomainService(
      CalendarFactory calendarFactory, CalendarRepository calendarRepository) {
    this.calendarFactory = calendarFactory;
    this.calendarRepository = calendarRepository;
  }

  /**
   * 导入生成：工作日历存在则修改，不存在则创建
   *
   * @param code 代码
   * @param name 名称
   * @param disabled 是否停用
   * @param remark 备注（可空）
   * @return 工作日历聚合
   */
  public Calendar importGenerate(
      String code, String name, boolean disabled, @Nullable String remark) {
    // 按代码查工作日历
    Calendar model = calendarRepository.getByCode(code);

    // 不存在则创建，存在则修改
    if (model == null) {
      model = calendarFactory.create(code, name, disabled, remark);
    } else {
      modify(model, code, name, disabled, remark);
    }

    return model;
  }

  /**
   * 修改工作日历
   *
   * @param id 工作日历Id
   * @param code 代码
   * @param name 名称
   * @param disabled 是否停用
   * @param remark 备注（可空）
   * @return 工作日历聚合
   * @throws DomainException 工作日历不存在时抛出
   */
  public Calendar modify(
      long id, String code, String name, boolean disabled, @Nullable String remark) {
    Calendar model = getExist(id);
    modify(model, code, name, disabled, remark);
    return model;
  }

  /**
   * 修改工作日历
   *
   * @param model 工作日历聚合
   * @param code 代码
   * @param name 名称
   * @param disabled 是否停用
   * @param remark 备注（可空）
   * @throws DomainException 工作日历代码与其它日历冲突时抛出
   */
  public void modify(
      Calendar model, String code, String name, boolean disabled, @Nullable String remark) {
    // 校验工作日历代码唯一
    if (calendarRepository.isExistByCodeExcludeId(code, model.getId())) {
      throw new DomainException("工作日历代码" + code + "已存在!");
    }
    model.modify(code, name, disabled, remark);
  }

  /**
   * 添加常规时段
   *
   * @param calendarId 工作日历Id
   * @param dayOfWeek 星期几编码
   * @param startTime 上班时间
   * @param endTime 下班时间
   * @throws DomainException 工作日历不存在、或与已有时段重叠时抛出
   */
  public void addAttendance(
      long calendarId, String dayOfWeek, LocalTime startTime, LocalTime endTime) {
    getExist(calendarId).addAttendance(dayOfWeek, startTime, endTime);
  }

  /**
   * 修改常规时段
   *
   * @param calendarId 工作日历Id
   * @param attendanceId 时段Id
   * @param dayOfWeek 星期几编码
   * @param startTime 上班时间
   * @param endTime 下班时间
   * @throws DomainException 工作日历或时段不存在、或与其它时段重叠时抛出
   */
  public void modifyAttendance(
      long calendarId,
      long attendanceId,
      String dayOfWeek,
      LocalTime startTime,
      LocalTime endTime) {
    getExist(calendarId).modifyAttendance(attendanceId, dayOfWeek, startTime, endTime);
  }

  /**
   * 删除常规时段（逻辑删除）
   *
   * @param calendarId 工作日历Id
   * @param attendanceId 时段Id
   * @throws DomainException 工作日历不存在时抛出
   */
  public void deleteAttendance(long calendarId, long attendanceId) {
    getExist(calendarId).deleteAttendance(attendanceId);
  }

  /**
   * 添加请假
   *
   * @param calendarId 工作日历Id
   * @param name 原因
   * @param startTime 开始时间
   * @param endTime 结束时间
   * @param remark 备注（可空）
   * @throws DomainException 工作日历不存在时抛出
   */
  public void addLeave(
      long calendarId,
      String name,
      LocalDateTime startTime,
      LocalDateTime endTime,
      @Nullable String remark) {
    getExist(calendarId).addLeave(name, startTime, endTime, remark);
  }

  /**
   * 修改请假
   *
   * @param calendarId 工作日历Id
   * @param leaveId 请假Id
   * @param name 原因
   * @param startTime 开始时间
   * @param endTime 结束时间
   * @param remark 备注（可空）
   * @throws DomainException 工作日历或请假不存在时抛出
   */
  public void modifyLeave(
      long calendarId,
      long leaveId,
      String name,
      LocalDateTime startTime,
      LocalDateTime endTime,
      @Nullable String remark) {
    getExist(calendarId).modifyLeave(leaveId, name, startTime, endTime, remark);
  }

  /**
   * 删除请假（逻辑删除）
   *
   * @param calendarId 工作日历Id
   * @param leaveId 请假Id
   * @throws DomainException 工作日历不存在时抛出
   */
  public void deleteLeave(long calendarId, long leaveId) {
    getExist(calendarId).deleteLeave(leaveId);
  }

  /**
   * 取工作日历，不存在则抛异常
   *
   * @param id 工作日历Id
   * @return 工作日历聚合
   * @throws DomainException 工作日历不存在时抛出
   */
  private Calendar getExist(long id) {
    Calendar model = calendarRepository.get(id);
    if (model == null) {
      throw new DomainException("工作日历" + id + "不存在!");
    }
    return model;
  }
}
