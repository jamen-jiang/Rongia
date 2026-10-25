package com.rongia.system.domain.privilege;

import com.rongia.shared.domain.repository.Repository;
import java.util.List;
import org.jspecify.annotations.NullMarked;

/** 权限仓储接口 */
@NullMarked
public interface PrivilegeRepository extends Repository<Privilege> {
  /**
   * 按主体查询权限列表
   *
   * @param master 主体
   * @param masterValue 主体值
   * @return 权限列表
   */
  List<Privilege> getListByMaster(String master, long masterValue);
}
