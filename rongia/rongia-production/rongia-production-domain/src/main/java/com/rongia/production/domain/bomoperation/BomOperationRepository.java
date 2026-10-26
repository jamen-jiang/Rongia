package com.rongia.production.domain.bomoperation;

import com.rongia.shared.domain.repository.Repository;
import java.util.List;
import org.jspecify.annotations.NullMarked;

/** 工序行仓储接口 */
@NullMarked
public interface BomOperationRepository extends Repository<BomOperation> {

  /**
   * 按父级查询工序行，按顺序排列（只含未删除）
   *
   * @param parentType 父级类型编码
   * @param parentId 父级Id
   * @return 工序行列表，没有时返回空列表
   */
  List<BomOperation> listByParent(String parentType, long parentId);

  /**
   * 判断父级下某个工序顺序是否已被占用
   *
   * @param parentType 父级类型编码
   * @param parentId 父级Id
   * @param sequence 工序顺序
   * @param excludeId 排除的工序行Id（可空，新增时传 null）
   * @return true 表示已存在
   */
  boolean isExistSequenceExcludeId(
      String parentType, long parentId, int sequence, Long excludeId);
}
