package com.rongia.system.infrastructure.employee;

import com.rongia.system.domain.employee.Employee;
import com.rongia.system.domain.employee.EmployeeRepository;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

/** 员工仓储实现 */
@NullMarked
@Repository
public class EmployeeRepositoryImpl implements EmployeeRepository {

  /** JPA 接口 */
  private final EmployeeJpa jpa;

  /**
   * 构造器注入
   *
   * @param jpa JPA 接口
   */
  public EmployeeRepositoryImpl(EmployeeJpa jpa) {
    this.jpa = jpa;
  }

  /**
   * 按主键查询
   *
   * @param id 主键
   * @return 员工，不存在返回 null
   */
  @Override
  @Nullable
  public Employee get(long id) {
    return jpa.findById(id).orElse(null);
  }

  /**
   * 按员工编号查询
   *
   * @param number 员工编号
   * @return 员工，不存在返回 null
   */
  @Override
  @Nullable
  public Employee getByNumber(String number) {
    return jpa.findByNumber(number).orElse(null);
  }

  /**
   * 按关联账号查询员工
   *
   * @param userId 账号Id
   * @return 员工列表
   */
  @Override
  public List<Employee> getListByUserId(long userId) {
    return jpa.findByUserId(userId);
  }

  /**
   * 判断员工编号是否已存在（可排除自身）
   *
   * @param number 员工编号
   * @param id 排除的员工Id（可空）
   * @return 是否存在
   */
  @Override
  public boolean isExistByNumberExcludeId(String number, @Nullable Long id) {
    return jpa.existsByNumberExcludeId(number, id);
  }

  /**
   * 保存（新增或更新）
   *
   * @param model 员工聚合
   */
  @Override
  public void save(Employee model) {
    jpa.save(model);
  }
}