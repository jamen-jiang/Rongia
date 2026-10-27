package com.rongia.stock.domain.warehouse;

import com.rongia.shared.domain.exception.DomainException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 仓库领域服务，负责跨聚合编排 */
@NullMarked
public class WarehouseDomainService {

  /** 仓库工厂 */
  private final WarehouseFactory warehouseFactory;

  /** 仓库仓储 */
  private final WarehouseRepository warehouseRepository;

  /**
   * 构造器注入
   *
   * @param warehouseFactory 仓库工厂
   * @param warehouseRepository 仓库仓储
   */
  public WarehouseDomainService(
      WarehouseFactory warehouseFactory, WarehouseRepository warehouseRepository) {
    this.warehouseFactory = warehouseFactory;
    this.warehouseRepository = warehouseRepository;
  }

  /**
   * 导入生成：仓库代码存在则修改，不存在则创建
   *
   * @param code 代码
   * @param name 名称
   * @param sequence 排序
   * @param disabled 是否停用
   * @param remark 备注（可空）
   * @return 仓库聚合
   */
  public Warehouse importGenerate(String code,
      String name,
      int sequence,
      boolean disabled,
      @Nullable String remark) {
    Warehouse model = warehouseRepository.getByCode(code);
    if (model == null) {
      return warehouseFactory.create(code, name, sequence, disabled, remark);
    }
    modify(model, code, name, sequence, disabled, remark);
    return model;
  }

  /**
   * 创建仓库
   *
   * @param code 代码
   * @param name 名称
   * @param sequence 排序
   * @param disabled 是否停用
   * @param remark 备注（可空）
   * @return 仓库聚合
   * @throws DomainException 仓库代码已存在时抛出
   */
  public Warehouse create(String code,
      String name,
      int sequence,
      boolean disabled,
      @Nullable String remark) {
    return warehouseFactory.create(code, name, sequence, disabled, remark);
  }

  /**
   * 修改仓库（按Id查询后修改）
   *
   * @param id 仓库Id
   * @param code 代码
   * @param name 名称
   * @param sequence 排序
   * @param disabled 是否停用
   * @param remark 备注（可空）
   * @return 仓库聚合
   * @throws DomainException 仓库不存在、或代码冲突时抛出
   */
  public Warehouse modify(
      long id, String code,
      String name,
      int sequence,
      boolean disabled,
      @Nullable String remark) {
    Warehouse model = getExist(id);
    modify(model, code, name, sequence, disabled, remark);
    return model;
  }

  /**
   * 修改仓库（使用已有聚合）
   *
   * @param model 仓库聚合
   * @param code 代码
   * @param name 名称
   * @param sequence 排序
   * @param disabled 是否停用
   * @param remark 备注（可空）
   * @throws DomainException 仓库代码冲突时抛出
   */
  public void modify(
      Warehouse model, String code,
      String name,
      int sequence,
      boolean disabled,
      @Nullable String remark) {
    if (warehouseRepository.isExistByCodeExcludeId(code, model.getId())) {
      throw new DomainException("仓库代码" + code + "已存在!");
    }
    model.modify(code, name, sequence, disabled, remark);
  }

  /**
   * 删除仓库（逻辑删除）
   *
   * @param id 仓库Id
   * @return 仓库聚合（已标记删除）
   * @throws DomainException 仓库不存在时抛出
   */
  public Warehouse delete(long id) {
    Warehouse model = getExist(id);
    model.delete();
    return model;
  }

  /**
   * 取仓库，不存在则抛异常
   *
   * @param id 仓库Id
   * @return 仓库
   * @throws DomainException 仓库不存在时抛出
   */
  private Warehouse getExist(long id) {
    Warehouse model = warehouseRepository.get(id);
    if (model == null) {
      throw new DomainException("仓库" + id + "不存在!");
    }
    return model;
  }
}
