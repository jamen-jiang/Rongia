package com.rongia.stock.domain.warehouse;

import com.rongia.shared.domain.exception.DomainException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 仓库工厂，负责创建仓库 */
@NullMarked
public class WarehouseFactory {

  /** 仓库仓储 */
  private final WarehouseRepository warehouseRepository;

  /**
   * 构造器注入
   *
   * @param warehouseRepository 仓库仓储
   */
  public WarehouseFactory(WarehouseRepository warehouseRepository) {
    this.warehouseRepository = warehouseRepository;
  }

  /**
   * 创建仓库
   *
   * @param parentId 上级仓库Id（可空）
   * @param parentPath 层级路径（可空）
   * @param isLeaf 是否叶子（叶子=存货点）
   * @param code 代码
   * @param name 名称
   * @param sequence 排序
   * @param disabled 是否停用
   * @param remark 备注（可空）
   * @return 仓库聚合
   * @throws DomainException 同级下代码已存在时抛出
   */
  public Warehouse create(
      @Nullable Long parentId,
      @Nullable String parentPath,
      boolean isLeaf,
      String code,
      String name,
      int sequence,
      boolean disabled,
      @Nullable String remark) {
    if (warehouseRepository.isExistByCodeExcludeId(parentId, code)) {
      throw new DomainException("仓库代码" + code + "在同级下已存在!");
    }
    return new Warehouse(parentId, parentPath, isLeaf, code, name, sequence, disabled, remark);
  }
}
