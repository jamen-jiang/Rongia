package com.rongia.stock.domain.warehouse;

import com.rongia.shared.domain.repository.Repository;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 仓库仓储接口 */
@NullMarked
public interface WarehouseRepository extends Repository<Warehouse> {

  /**
   * 按「上级 + 代码」查询
   *
   * @param parentId 上级仓库Id（可空，根节点传 null）
   * @param code 仓库代码
   * @return 仓库（可空），不存在返回 null
   */
  @Nullable Warehouse getByCode(@Nullable Long parentId, String code);

  /**
   * 判断同级下仓库代码是否存在
   *
   * @param parentId 上级仓库Id（可空，根节点传 null）
   * @param code 仓库代码
   * @param id 排除的仓库Id（可空，新增时传 null）
   * @return true 表示已存在
   */
  boolean isExistByCodeExcludeId(@Nullable Long parentId, String code, @Nullable Long id);

  /**
   * 判断同级下仓库代码是否存在
   *
   * @param parentId 上级仓库Id（可空，根节点传 null）
   * @param code 仓库代码
   * @return true 表示已存在
   */
  default boolean isExistByCodeExcludeId(@Nullable Long parentId, String code) {
    return isExistByCodeExcludeId(parentId, code, null);
  }

  /**
   * 查一个仓库下面的直接子仓库
   *
   * @param parentId 上级仓库Id
   * @return 子仓库列表
   */
  List<Warehouse> getListByParentId(long parentId);
}
