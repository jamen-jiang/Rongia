package com.rongia.system.infrastructure.company;

import com.rongia.system.domain.company.Company;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/** 公司 JPA 接口 */
@NullMarked
@Repository
public interface CompanyJpa
    extends JpaRepository<Company, Long>, JpaSpecificationExecutor<Company> {

  /**
   * 查询指定上级下的所有直接下级公司
   *
   * @param parentId 上级公司Id
   * @return 公司列表
   */
  List<Company> findByParentId(long parentId);

  /**
   * 查询指定公司及其所有子孙公司（包含自身）
   *
   * @param id 公司Id
   * @return 公司及其子孙列表
   */
  default List<Company> findWithChildren(long id) {
    Specification<Company> spec =
        (root, query, cb) ->
            cb.or(
                cb.equal(root.get("id"), id),
                cb.like(cb.concat(cb.concat("-", root.get("parentPath")), "-"), "%-" + id + "-%"));
    return findAll(spec);
  }

  /**
   * 判断公司代码是否存在（可排除指定 Id）
   *
   * @param code 公司代码
   * @param id 排除的Id（可空，null 表示不排除）
   * @return true 表示已存在
   */
  default boolean existsByCodeExcludeId(String code, @Nullable Long id) {
    Specification<Company> spec =
        (root, query, cb) -> {
          List<Predicate> predicates = new ArrayList<>();
          predicates.add(cb.equal(root.get("code"), code));
          if (id != null) {
            predicates.add(cb.notEqual(root.get("id"), id));
          }
          return cb.and(predicates.toArray(new Predicate[0]));
        };
    return exists(spec);
  }

  /**
   * 判断同一上级下公司名称是否存在（可排除指定 Id）
   *
   * @param name 公司名称
   * @param parentId 上级公司Id（可空，null 表示顶级）
   * @param id 排除的Id（可空，null 表示不排除）
   * @return true 表示已存在
   */
  default boolean existsByNameAndParentIdExcludeId(
      String name, @Nullable Long parentId, @Nullable Long id) {
    Specification<Company> spec =
        (root, query, cb) -> {
          List<Predicate> predicates = new ArrayList<>();
          predicates.add(cb.equal(root.get("name"), name));
          if (parentId == null) {
            predicates.add(cb.isNull(root.get("parentId")));
          } else {
            predicates.add(cb.equal(root.get("parentId"), parentId));
          }
          if (id != null) {
            predicates.add(cb.notEqual(root.get("id"), id));
          }
          return cb.and(predicates.toArray(new Predicate[0]));
        };
    return exists(spec);
  }
}