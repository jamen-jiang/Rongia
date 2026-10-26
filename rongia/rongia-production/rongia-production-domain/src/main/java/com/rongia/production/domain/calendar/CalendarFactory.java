package com.rongia.production.domain.calendar;

import com.rongia.shared.domain.exception.DomainException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 工作日历工厂，负责创建工作日历 */
@NullMarked
public class CalendarFactory {

  /** 工作日历仓储 */
  private final CalendarRepository calendarRepository;

  /**
   * 构造器注入
   *
   * @param calendarRepository 工作日历仓储
   */
  public CalendarFactory(CalendarRepository calendarRepository) {
    this.calendarRepository = calendarRepository;
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
  public Calendar create(
      String code, String name, boolean disabled, @Nullable String remark) {
    // 校验工作日历代码唯一
    if (calendarRepository.isExistByCodeExcludeId(code)) {
      throw new DomainException("工作日历代码" + code + "已存在!");
    }
    return new Calendar(code, name, disabled, remark);
  }
}
