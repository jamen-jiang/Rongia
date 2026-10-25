package com.rongia.system.domain.employee;

import com.rongia.shared.domain.repository.Repository;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 员工仓储接口 */
@NullMarked
public interface EmployeeRepository extends Repository<Employee> {

  /**
   * 按员工编号查询
   *
   * @param number 员工编号
   * @return 员工（可空），不存在返回 null
   */
  @Nullable Employee getByNumber(String number);

  /**
   * 按关联账号查询员工（登录时算数据范围用）
   *
   * @param userId 账号Id
   * @return 员工列表（一个账号理论上对应一个员工，用列表避免强假设）
   */
  List<Employee> getListByUserId(long userId);

  /**
   * 判断员工编号是否存在
   *
   * @param number 员工编号
   * @param id 排除的员工Id（可空，新增时传 null）
   * @return true 表示已存在
   */
  boolean isExistByNumberExcludeId(String number, @Nullable Long id);

  /**
   * 判断员工编号是否存在（新增时校验）
   *
   * @param number 员工编号
   * @return true 表示已存在
   */
  default boolean isExistByNumberExcludeId(String number) {
    return isExistByNumberExcludeId(number, null);
  }
}