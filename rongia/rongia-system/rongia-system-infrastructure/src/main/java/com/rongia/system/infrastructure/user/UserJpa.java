package com.rongia.system.infrastructure.user;

import com.rongia.system.domain.user.User;
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

/** 用户 JPA 接口 */
@NullMarked
@Repository
public interface UserJpa
    extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

  /**
   * 按账号查询
   *
   * @param userName 账号
   * @return 用户（可能为空）
   */
  Optional<User> findByUserName(String userName);

  /**
   * 判断用户名称是否已存在（可排除指定Id）
   *
   * @param name 用户名称
   * @param id 排除的用户Id（可空）
   * @return true 表示已存在
   */
  default boolean existsByNameExcludeId(String name, @Nullable Long id) {
    return exists(buildExcludeIdSpec("name", name, id));
  }

  /**
   * 判断账号是否已存在（可排除指定Id）
   *
   * @param userName 账号
   * @param id 排除的用户Id（可空）
   * @return true 表示已存在
   */
  default boolean existsByUserNameExcludeId(String userName, @Nullable Long id) {
    return exists(buildExcludeIdSpec("userName", userName, id));
  }

  /**
   * 判断邮箱是否已存在（可排除指定Id）
   *
   * @param email 邮箱（可空）
   * @param id 排除的用户Id（可空）
   * @return true 表示已存在
   */
  default boolean existsByEmailExcludeId(@Nullable String email, @Nullable Long id) {
    return exists(buildExcludeIdSpec("email", email, id));
  }

  /**
   * 构造「字段相等 + 排除Id」条件
   *
   * @param field 字段名（name / userName / email）
   * @param value 字段值（可空）
   * @param id 排除的用户Id（可空）
   * @return 查询条件
   */
  private Specification<User> buildExcludeIdSpec(
      String field, @Nullable String value, @Nullable Long id) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(cb.equal(root.get(field), value));
      if (id != null) {
        predicates.add(cb.notEqual(root.get("id"), id));
      }
      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }
}
