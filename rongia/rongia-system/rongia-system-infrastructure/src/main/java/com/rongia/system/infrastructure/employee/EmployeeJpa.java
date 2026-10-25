package com.rongia.system.infrastructure.employee;

import com.rongia.system.domain.employee.Employee;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/** 员工 JPA 接口 */
@NullMarked
@Repository
public interface EmployeeJpa
    extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

  /**
   * 按员工编号查询
   *
   * @param number 员工编号
   * @return 员工（可能为空）
   */
  Optional<Employee> findByNumber(String number);

  /**
   * 按关联账号查询员工
   *
   * @param userId 账号Id
   * @return 员工列表（一个账号理论上对应一个员工，用列表避免强假设）
   */
  List<Employee> findByUserId(long userId);

  /**
   * 判断员工编号是否已存在（可排除指定Id）
   *
   * @param number 员工编号
   * @param id 排除的员工Id（可空，null 表示不排除）
   * @return true 表示已存在
   */
  default boolean existsByNumberExcludeId(String number, @Nullable Long id) {
    Specification<Employee> spec =
        (root, query, cb) -> {
          List<Predicate> predicates = new ArrayList<>();
          predicates.add(cb.equal(root.get("number"), number));
          if (id != null) {
            predicates.add(cb.notEqual(root.get("id"), id));
          }
          return cb.and(predicates.toArray(new Predicate[0]));
        };
    return exists(spec);
  }
}