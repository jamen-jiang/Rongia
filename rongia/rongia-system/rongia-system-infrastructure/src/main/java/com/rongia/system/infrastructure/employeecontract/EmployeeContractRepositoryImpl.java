package com.rongia.system.infrastructure.employeecontract;

import com.rongia.system.domain.employeecontract.EmployeeContract;
import com.rongia.system.domain.employeecontract.EmployeeContractRepository;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

/** 劳动合同仓储实现 */
@NullMarked
@Repository
public class EmployeeContractRepositoryImpl implements EmployeeContractRepository {

  /** JPA 接口 */
  private final EmployeeContractJpa jpa;

  /**
   * 构造器注入
   *
   * @param jpa JPA 接口
   */
  public EmployeeContractRepositoryImpl(EmployeeContractJpa jpa) {
    this.jpa = jpa;
  }

  /**
   * 按主键查询
   *
   * @param id 主键
   * @return 合同，不存在返回 null
   */
  @Override
  @Nullable
  public EmployeeContract get(long id) {
    return jpa.findById(id).orElse(null);
  }

  /**
   * 按合同编号查询
   *
   * @param number 合同编号
   * @return 合同，不存在返回 null
   */
  @Override
  @Nullable
  public EmployeeContract getByNumber(String number) {
    return jpa.findByNumber(number).orElse(null);
  }

  /**
   * 按员工查询合同列表
   *
   * @param employeeId 员工Id
   * @return 合同列表
   */
  @Override
  public List<EmployeeContract> getListByEmployeeId(long employeeId) {
    return jpa.findByEmployeeId(employeeId);
  }

  /**
   * 判断合同编号是否已存在（可排除自身）
   *
   * @param number 合同编号
   * @param id 排除的合同Id（可空）
   * @return 是否存在
   */
  @Override
  public boolean isExistByNumberExcludeId(String number, @Nullable Long id) {
    return jpa.existsByNumberExcludeId(number, id);
  }

  /**
   * 保存（新增或更新）
   *
   * @param model 合同聚合
   */
  @Override
  public void save(EmployeeContract model) {
    jpa.save(model);
  }
}