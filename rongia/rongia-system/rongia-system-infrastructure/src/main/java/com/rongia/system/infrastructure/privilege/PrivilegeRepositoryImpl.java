package com.rongia.system.infrastructure.privilege;

import com.rongia.system.domain.privilege.Privilege;
import com.rongia.system.domain.privilege.PrivilegeRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

/** 权限仓储实现 */
@NullMarked
@Repository
public class PrivilegeRepositoryImpl implements PrivilegeRepository {

  /** JPA 接口 */
  private final PrivilegeJpa jpa;

  /**
   * 构造器注入
   *
   * @param jpa JPA 接口
   */
  public PrivilegeRepositoryImpl(PrivilegeJpa jpa) {
    this.jpa = jpa;
  }

  /**
   * 按主键查询
   *
   * @param id 主键
   * @return 权限，不存在返回 null
   */
  @Override
  @Nullable
  public Privilege get(long id) {
    return jpa.findById(id).orElse(null);
  }

  /**
   * 按主体 + 主体值查询权限列表
   *
   * @param master 主体
   * @param masterValue 主体值
   * @return 权限列表
   */
  @Override
  public List<Privilege> getListByMaster(String master, long masterValue) {
    Specification<Privilege> spec =
        (root, query, cb) -> {
          List<Predicate> predicates = new ArrayList<>();
          predicates.add(cb.equal(root.get("master"), master));
          predicates.add(cb.equal(root.get("masterValue"), masterValue));
          return cb.and(predicates.toArray(new Predicate[0]));
        };
    return jpa.findAll(spec);
  }

  /**
   * 保存（新增或更新）
   *
   * @param model 权限聚合
   */
  @Override
  public void save(Privilege model) {
    jpa.save(model);
  }
}
