package com.rongia.system.infrastructure.serialnumber;

import com.rongia.system.domain.serialnumber.SerialNumberRule;
import jakarta.persistence.LockModeType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** 流水号规则 JPA 接口 */
@NullMarked
@Repository
public interface SerialNumberRuleJpa
    extends JpaRepository<SerialNumberRule, Long>, JpaSpecificationExecutor<SerialNumberRule> {

  /**
   * 按类型查询规则
   *
   * @param type 类型
   * @return 规则，不存在返回 null
   */
  @Nullable SerialNumberRule findByType(@Nullable String type);

  /**
   * 按类型悲观锁查询规则（消费计数用，必须在事务中调用）
   *
   * @param type 类型
   * @return 规则，不存在返回 null
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from SerialNumberRule r where r.type = :type")
  @Nullable SerialNumberRule findByTypeForUpdate(@Param("type") @Nullable String type);

  /**
   * 判断类型是否已存在（可排除指定 Id）
   *
   * @param type 类型
   * @param id 排除的Id（可空，null 表示不排除）
   * @return true 表示已存在
   */
  default boolean existsByTypeExcludeId(@Nullable String type, @Nullable Long id) {
    Specification<SerialNumberRule> spec =
        (root, query, cb) -> {
          List<Predicate> predicates = new ArrayList<>();
          predicates.add(cb.equal(root.get("type"), type));
          if (id != null) {
            predicates.add(cb.notEqual(root.get("id"), id));
          }
          return cb.and(predicates.toArray(new Predicate[0]));
        };
    return exists(spec);
  }
}
