package com.rongia.production.domain.calendar;

import com.rongia.shared.domain.repository.Repository;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 工作日历仓储接口 */
@NullMarked
public interface CalendarRepository extends Repository<Calendar> {

  /**
   * 按代码查询
   *
   * @param code 工作日历代码
   * @return 工作日历（可空），不存在返回 null
   */
  @Nullable Calendar getByCode(String code);

  /**
   * 判断工作日历代码是否存在
   *
   * @param code 工作日历代码
   * @param id 排除的工作日历Id（可空，新增时传 null）
   * @return true 表示已存在
   */
  boolean isExistByCodeExcludeId(String code, @Nullable Long id);

  /**
   * 判断工作日历代码是否存在
   *
   * @param code 工作日历代码
   * @return true 表示已存在
   */
  default boolean isExistByCodeExcludeId(String code) {
    return isExistByCodeExcludeId(code, null);
  }
}
