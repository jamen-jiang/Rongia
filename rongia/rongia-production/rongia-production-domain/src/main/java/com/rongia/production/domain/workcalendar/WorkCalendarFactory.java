package com.rongia.production.domain.workcalendar;

import com.rongia.shared.domain.exception.DomainException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 工作日历工厂，负责创建工作日历 */
@NullMarked
public class WorkCalendarFactory {

  /** 工作日历仓储 */
  private final WorkCalendarRepository workCalendarRepository;

  /**
   * 构造器注入
   *
   * @param workCalendarRepository 工作日历仓储
   */
  public WorkCalendarFactory(WorkCalendarRepository workCalendarRepository) {
    this.workCalendarRepository = workCalendarRepository;
  }

  /**
   * 创建工作日历
   *
   * @param code 代码
   * @param name 名称
   * @param disabled 是否停用
   * @param remark 备注（可空）
   * @return 工作日历聚合
   * @throws DomainException 工作日历代码已存在时抛出
   */
  public WorkCalendar create(
      String code, String name, boolean disabled, @Nullable String remark) {
    // 校验工作日历代码唯一
    if (workCalendarRepository.isExistByCodeExcludeId(code)) {
      throw new DomainException("工作日历代码" + code + "已存在!");
    }
    return new WorkCalendar(code, name, disabled, remark);
  }
}
