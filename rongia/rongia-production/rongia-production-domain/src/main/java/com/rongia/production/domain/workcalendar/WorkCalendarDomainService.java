package com.rongia.production.domain.workcalendar;

import com.rongia.shared.domain.exception.DomainException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 工作日历领域服务，负责跨聚合编排与子行同步 */
@NullMarked
public class WorkCalendarDomainService {

  /** 工作日历工厂 */
  private final WorkCalendarFactory workCalendarFactory;

  /** 工作日历仓储 */
  private final WorkCalendarRepository workCalendarRepository;

  /**
   * 构造器注入
   *
   * @param workCalendarFactory 工作日历工厂
   * @param workCalendarRepository 工作日历仓储
   */
  public WorkCalendarDomainService(
      WorkCalendarFactory workCalendarFactory, WorkCalendarRepository workCalendarRepository) {
    this.workCalendarFactory = workCalendarFactory;
    this.workCalendarRepository = workCalendarRepository;
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
  public WorkCalendar importGenerate(
      String code, String name, boolean disabled, @Nullable String remark) {
    // 按代码查工作日历
    WorkCalendar model = workCalendarRepository.getByCode(code);

    // 不存在则创建，存在则修改
    if (model == null) {
      model = workCalendarFactory.create(code, name, disabled, remark);
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
  public WorkCalendar modify(
      long id, String code, String name, boolean disabled, @Nullable String remark) {
    WorkCalendar model = getExist(id);
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
      WorkCalendar model, String code, String name, boolean disabled, @Nullable String remark) {
    // 校验工作日历代码唯一
    if (workCalendarRepository.isExistByCodeExcludeId(code, model.getId())) {
      throw new DomainException("工作日历代码" + code + "已存在!");
    }
    model.modify(code, name, disabled, remark);
  }

  /**
   * 添加常规时段
   *
   * @param workCalendarId 工作日历Id
   * @param dayOfWeek 星期几编码
   * @param startTime 上班时间
   * @param endTime 下班时间
   * @throws DomainException 工作日历不存在、或与已有时段重叠时抛出
   */
  public void addSlot(
      long workCalendarId, String dayOfWeek, LocalTime startTime, LocalTime endTime) {
    getExist(workCalendarId).addSlot(dayOfWeek, startTime, endTime);
  }

  /**
   * 修改常规时段
   *
   * @param workCalendarId 工作日历Id
   * @param slotId 时段Id
   * @param dayOfWeek 星期几编码
   * @param startTime 上班时间
   * @param endTime 下班时间
   * @throws DomainException 工作日历或时段不存在、或与其它时段重叠时抛出
   */
  public void modifySlot(
      long workCalendarId,
      long slotId,
      String dayOfWeek,
      LocalTime startTime,
      LocalTime endTime) {
    getExist(workCalendarId).modifySlot(slotId, dayOfWeek, startTime, endTime);
  }

  /**
   * 删除常规时段（逻辑删除）
   *
   * @param workCalendarId 工作日历Id
   * @param slotId 时段Id
   * @throws DomainException 工作日历不存在时抛出
   */
  public void deleteSlot(long workCalendarId, long slotId) {
    getExist(workCalendarId).deleteSlot(slotId);
  }

  /**
   * 添加例外
   *
   * @param workCalendarId 工作日历Id
   * @param name 原因
   * @param startTime 开始时间
   * @param endTime 结束时间
   * @param remark 备注（可空）
   * @throws DomainException 工作日历不存在时抛出
   */
  public void addException(
      long workCalendarId,
      String name,
      LocalDateTime startTime,
      LocalDateTime endTime,
      @Nullable String remark) {
    getExist(workCalendarId).addException(name, startTime, endTime, remark);
  }

  /**
   * 修改例外
   *
   * @param workCalendarId 工作日历Id
   * @param exceptionId 例外Id
   * @param name 原因
   * @param startTime 开始时间
   * @param endTime 结束时间
   * @param remark 备注（可空）
   * @throws DomainException 工作日历或例外不存在时抛出
   */
  public void modifyException(
      long workCalendarId,
      long exceptionId,
      String name,
      LocalDateTime startTime,
      LocalDateTime endTime,
      @Nullable String remark) {
    getExist(workCalendarId).modifyException(exceptionId, name, startTime, endTime, remark);
  }

  /**
   * 删除例外（逻辑删除）
   *
   * @param workCalendarId 工作日历Id
   * @param exceptionId 例外Id
   * @throws DomainException 工作日历不存在时抛出
   */
  public void deleteException(long workCalendarId, long exceptionId) {
    getExist(workCalendarId).deleteException(exceptionId);
  }

  /**
   * 取工作日历，不存在则抛异常
   *
   * @param id 工作日历Id
   * @return 工作日历聚合
   * @throws DomainException 工作日历不存在时抛出
   */
  private WorkCalendar getExist(long id) {
    WorkCalendar model = workCalendarRepository.get(id);
    if (model == null) {
      throw new DomainException("工作日历" + id + "不存在!");
    }
    return model;
  }
}
