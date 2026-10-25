package com.rongia.system.domain.employeecontract;

import com.rongia.shared.domain.repository.Repository;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 劳动合同仓储接口 */
@NullMarked
public interface EmployeeContractRepository extends Repository<EmployeeContract> {

  /**
   * 按合同编号查询
   *
   * @param number 合同编号
   * @return 合同（可空），不存在返回 null
   */
  @Nullable EmployeeContract getByNumber(String number);

  /**
   * 按员工查询合同列表
   *
   * @param employeeId 员工Id
   * @return 合同列表
   */
  List<EmployeeContract> getListByEmployeeId(long employeeId);

  /**
   * 判断合同编号是否存在
   *
   * @param number 合同编号
   * @param id 排除的合同Id（可空，新增时传 null）
   * @return true 表示已存在
   */
  boolean isExistByNumberExcludeId(String number, @Nullable Long id);

  /**
   * 判断合同编号是否存在（新增时校验）
   *
   * @param number 合同编号
   * @return true 表示已存在
   */
  default boolean isExistByNumberExcludeId(String number) {
    return isExistByNumberExcludeId(number, null);
  }
}