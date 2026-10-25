package com.rongia.system.infrastructure.role;

import com.rongia.system.domain.role.Role;
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

/** 角色 JPA 接口 */
@NullMarked
@Repository
public interface RoleJpa extends JpaRepository<Role, Long>, JpaSpecificationExecutor<Role> {

  /**
   * 按角色名称查询
   *
   * @param name 角色名称
   * @return 角色（可能为空）
   */
  Optional<Role> findByName(String name);

  /**
   * 判断角色名称是否已存在（可排除指定Id）
   *
   * @param name 角色名称
   * @param id 排除的角色Id（可空）
   * @return true 表示已存在
   */
  default boolean existsByNameExcludeId(String name, @Nullable Long id) {
    Specification<Role> spec =
        (root, query, cb) -> {
          List<Predicate> predicates = new ArrayList<>();
          predicates.add(cb.equal(root.get("name"), name));
          if (id != null) {
            predicates.add(cb.notEqual(root.get("id"), id));
          }
          return cb.and(predicates.toArray(new Predicate[0]));
        };
    return exists(spec);
  }
}
