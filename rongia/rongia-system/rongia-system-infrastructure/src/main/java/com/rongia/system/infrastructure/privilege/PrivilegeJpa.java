package com.rongia.system.infrastructure.privilege;

import com.rongia.system.domain.privilege.Privilege;
import java.util.Set;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/** 权限 JPA 接口 */
@NullMarked
@Repository
public interface PrivilegeJpa
    extends JpaRepository<Privilege, Long>, JpaSpecificationExecutor<Privilege> {

  /**
   * 按主体 + 主体值集合查权限
   *
   * @param master 主体（User / Role）
   * @param masterValues 主体值集合
   * @return 权限集合
   */
  Set<Privilege> findByMasterAndMasterValueIn(String master, Set<Long> masterValues);
}
