package com.rongia.system.infrastructure.employeecontract;

import com.rongia.system.domain.employeecontract.EmployeeContract;
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

/** 劳动合同 JPA 接口 */
@NullMarked
@Repository
public interface EmployeeContractJpa
    extends JpaRepository<EmployeeContract, Long>, JpaSpecificationExecutor<EmployeeContract> {

  /**
   * 按合同编号查询
   *
   * @param number 合同编号
   * @return 合同（可能为空）
   */
  Optional<EmployeeContract> findByNumber(String number);

  /**
   * 按员工查询合同列表
   *
   * @param employeeId 员工Id
   * @return 合同列表
   */
  List<EmployeeContract> findByEmployeeId(long employeeId);

  /**
   * 判断合同编号是否已存在（可排除指定Id）
   *
   * @param number 合同编号
   * @param id 排除的合同Id（可空，null 表示不排除）
   * @return true 表示已存在
   */
  default boolean existsByNumberExcludeId(String number, @Nullable Long id) {
    Specification<EmployeeContract> spec =
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