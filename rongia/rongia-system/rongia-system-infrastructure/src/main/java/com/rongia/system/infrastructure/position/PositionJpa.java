package com.rongia.system.infrastructure.position;

import com.rongia.system.domain.position.Position;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/** 岗位 JPA 接口 */
@NullMarked
@Repository
public interface PositionJpa extends JpaRepository<Position, Long>, JpaSpecificationExecutor<Position> {

  /**
   * 按岗位名称查询
   *
   * @param name 岗位名称
   * @return 岗位（可能为空）
   */
  Optional<Position> findByName(String name);

  /**
   * 按部门Id集合批量查询
   *
   * @param departmentIds 部门Id集合
   * @return 岗位列表
   */
  List<Position> findByDepartmentIdIn(Collection<Long> departmentIds);

  /**
   * 查询指定上级下的所有直接下级岗位
   *
   * @param parentId 上级岗位Id
   * @return 岗位列表
   */
  List<Position> findByParentId(long parentId);

  /**
   * 查询指定岗位及其所有子孙岗位（包含自身）
   *
   * @param id 岗位Id
   * @return 岗位及其子孙列表
   */
  default List<Position> findWithChildren(long id) {
    Specification<Position> spec =
        (root, query, cb) ->
            cb.or(
                cb.equal(root.get("id"), id),
                cb.like(cb.concat(cb.concat("-", root.get("parentPath")), "-"), "%-" + id + "-%"));
    return findAll(spec);
  }

  /**
   * 判断岗位名称是否已存在（可排除指定Id）
   *
   * @param name 岗位名称
   * @param id 排除的岗位Id（可空）
   * @return true 表示已存在
   */
  default boolean existsByNameExcludeId(String name, @Nullable Long id) {
    Specification<Position> spec =
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
