package com.rongia.stock.domain.location;

import com.rongia.shared.domain.exception.DomainException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 库位工厂，负责创建库位 */
@NullMarked
public class LocationFactory {

  /** 库位仓储 */
  private final LocationRepository locationRepository;

  /**
   * 构造器注入
   *
   * @param locationRepository 库位仓储
   */
  public LocationFactory(LocationRepository locationRepository) {
    this.locationRepository = locationRepository;
  }

  /**
   * 创建库位
   *
   * @param warehouseId 所属仓库Id
   * @param parentId 上级库位Id（可空）
   * @param parentPath 层级路径（可空）
   * @param code 库位代码
   * @param name 库位名称
   * @param disabled 是否停用
   * @param remark 备注（可空）
   * @return 库位聚合
   * @throws DomainException 同一仓库下代码已存在时抛出
   */
  public Location create(
      long warehouseId,
      @Nullable Long parentId,
      @Nullable String parentPath,
      String code,
      String name,
      boolean disabled,
      @Nullable String remark) {
    if (locationRepository.isExistByCodeExcludeId(warehouseId, code)) {
      throw new DomainException("库位代码" + code + "在该仓库下已存在!");
    }
    return new Location(
        warehouseId, parentId, parentPath, code, name, disabled, remark);
  }
}
