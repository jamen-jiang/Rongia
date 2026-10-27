package com.rongia.stock.domain.location;

import com.rongia.shared.domain.repository.Repository;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 库位仓储接口 */
@NullMarked
public interface LocationRepository extends Repository<Location> {

  /**
   * 按「仓库 + 代码」查询库位
   *
   * @param warehouseId 仓库Id
   * @param code 库位代码
   * @return 库位（可空），不存在返回 null
   */
  @Nullable Location getByCode(long warehouseId, String code);

  /**
   * 查询某个仓库下的全部库位
   *
   * @param warehouseId 仓库Id
   * @return 库位列表，没有时返回空列表
   */
  List<Location> getListByWarehouseId(long warehouseId);

  /**
   * 按上级库位查询子库位
   *
   * @param parentId 上级库位Id
   * @return 子库位列表
   */
  List<Location> getListByParentId(long parentId);

  /**
   * 判断同一仓库下代码是否已存在
   *
   * @param warehouseId 仓库Id
   * @param code 库位代码
   * @param id 排除的库位Id（可空，新增时传 null）
   * @return true 表示已存在
   */
  boolean isExistByCodeExcludeId(long warehouseId, String code, @Nullable Long id);

  /**
   * 判断同一仓库下代码是否已存在
   *
   * @param warehouseId 仓库Id
   * @param code 库位代码
   * @return true 表示已存在
   */
  default boolean isExistByCodeExcludeId(long warehouseId, String code) {
    return isExistByCodeExcludeId(warehouseId, code, null);
  }
}
