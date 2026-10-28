package com.rongia.stock.domain.location;

import com.rongia.shared.domain.exception.DomainException;
import com.rongia.shared.domain.util.HierarchyUtils;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 库位领域服务，负责树形编排与跨聚合编排 */
@NullMarked
public class LocationDomainService {

  /** 库位工厂 */
  private final LocationFactory locationFactory;

  /** 库位仓储 */
  private final LocationRepository locationRepository;

  /**
   * 构造器注入
   *
   * @param locationFactory 库位工厂
   * @param locationRepository 库位仓储
   */
  public LocationDomainService(
      LocationFactory locationFactory, LocationRepository locationRepository) {
    this.locationFactory = locationFactory;
    this.locationRepository = locationRepository;
  }

  /**
   * 导入生成：同一仓库下代码已存在则修改，不存在则创建
   *
   * @param warehouseId 所属仓库Id
   * @param parentId 上级库位Id（可空）
   * @param code 库位代码
   * @param name 库位名称
   * @param disabled 是否停用
   * @param remark 备注（可空）
   * @return 库位聚合
   */
  public Location importGenerate(
      long warehouseId,
      @Nullable Long parentId,
      String code,
      String name,
      boolean disabled,
      @Nullable String remark) {
    Location model = locationRepository.getByCode(warehouseId, code);
    if (model == null) {
      return create(warehouseId, parentId, code, name, disabled, remark);
    }
    modify(model, warehouseId, parentId, code, name, disabled, remark);
    return model;
  }

  /**
   * 创建库位
   *
   * @param warehouseId 所属仓库Id
   * @param parentId 上级库位Id（可空）
   * @param code 库位代码
   * @param name 库位名称
   * @param disabled 是否停用
   * @param remark 备注（可空）
   * @return 库位聚合
   * @throws DomainException 上级库位不存在、或上级是结构节点以外的叶子时抛出
   */
  public Location create(
      long warehouseId,
      @Nullable Long parentId,
      String code,
      String name,
      boolean disabled,
      @Nullable String remark) {
    Location parent = parentId == null ? null : requireLocation(parentId);
    if (parent != null && !Objects.equals(parent.getWarehouseId(), warehouseId)) {
      throw new DomainException("上级库位" + parent.getName() + "不属于本仓库!");
    }
    return locationFactory.create(
        warehouseId,
        parentId,
        HierarchyUtils.resolveParentPath(parentId, parent, "上级库位"),
        code,
        name,
        disabled,
        remark);
  }

  /**
   * 修改库位（按Id查询后修改）
   *
   * @param id 库位Id
   * @param warehouseId 所属仓库Id
   * @param parentId 上级库位Id（可空）
   * @param code 库位代码
   * @param name 库位名称
   * @param disabled 是否停用
   * @param remark 备注（可空）
   * @return 库位聚合
   * @throws DomainException 库位不存在、代码冲突、或层级关系成环时抛出
   */
  public Location modify(
      long id,
      long warehouseId,
      @Nullable Long parentId,
      String code,
      String name,
      boolean disabled,
      @Nullable String remark) {
    Location model = getExist(id);
    modify(model, warehouseId, parentId, code, name, disabled, remark);
    return model;
  }

  /**
   * 修改库位（使用已有聚合）
   *
   * @param model 库位
   * @param warehouseId 所属仓库Id
   * @param parentId 上级库位Id（可空）
   * @param code 库位代码
   * @param name 库位名称
   * @param disabled 是否停用
   * @param remark 备注（可空）
   * @throws DomainException 代码冲突、或层级关系成环时抛出
   */
  public void modify(
      Location model,
      long warehouseId,
      @Nullable Long parentId,
      String code,
      String name,
      boolean disabled,
      @Nullable String remark) {
    if (locationRepository.isExistByCodeExcludeId(warehouseId, code, model.getId())) {
      throw new DomainException("库位代码" + code + "在该仓库下已存在!");
    }
    Location parent = parentId == null ? null : requireLocation(parentId);
    if (parent != null) {
      if (Objects.equals(parent.getId(), model.getId())) {
        throw new DomainException("库位的上级库位不能是自己");
      }
      if (!Objects.equals(parent.getWarehouseId(), warehouseId)) {
        throw new DomainException("上级库位" + parent.getName() + "不属于本仓库!");
      }
      if (isDescendant(model, parent)) {
        throw new DomainException("上级库位" + parent.getCode() + "是本库位的下级，不能反过来挂");
      }
    }
    model.modify(
        warehouseId,
        parentId,
        HierarchyUtils.resolveParentPath(parentId, parent, "上级库位"),
        code,
        name,
        disabled,
        remark);
    refreshDescendants(model);
  }

  /**
   * 删除库位（逻辑删除）
   *
   * @param id 库位Id
   * @return 库位聚合（已标记删除）
   * @throws DomainException 库位不存在、或下面还有子库位时抛出
   */
  public Location delete(long id) {
    Location model = getExist(id);
    if (!locationRepository.getListByParentId(model.getId()).isEmpty()) {
      throw new DomainException("库位" + model.getCode() + "下面还有子库位，不能删除");
    }
    model.delete();
    return model;
  }

  /**
   * 查一个库位下面的直接子库位
   *
   * @param parentId 上级库位Id
   * @return 子库位列表
   */
  public List<Location> getChildren(long parentId) {
    return locationRepository.getListByParentId(parentId);
  }

  /**
   * 取库位，不存在则抛异常
   *
   * @param id 库位Id
   * @return 库位聚合
   * @throws DomainException 不存在时抛出
   */
  public Location getExist(long id) {
    Location model = locationRepository.get(id);
    if (model == null) {
      throw new DomainException("库位" + id + "不存在!");
    }
    return model;
  }

  // ==================== 私有：树维护与校验 ====================

  /**
   * 递归刷新下级库位的层级路径
   *
   * @param model 已刷新过自身数据的库位
   */
  private void refreshDescendants(Location model) {
    List<Location> children = locationRepository.getListByParentId(model.getId());
    if (children.isEmpty()) {
      return;
    }
    HierarchyUtils.resetChildrenParentPath(model, children);
    for (Location child : children) {
      refreshDescendants(child);
    }
  }

  /**
   * 判断 candidate 是不是 unit 的子孙
   *
   * @param unit 本库位
   * @param candidate 待检查的库位（新上级）
   * @return true 表示 candidate 在 unit 下面
   */
  private boolean isDescendant(Location unit, Location candidate) {
    Location current = candidate;
    int depth = 0;
    final int maxDepth = 50;
    while (current != null && depth < maxDepth) {
      if (Objects.equals(current.getId(), unit.getId())) {
        return true;
      }
      current =
          current.getParentId() == null ? null : locationRepository.get(current.getParentId());
      depth++;
    }
    return false;
  }

  /**
   * 取库位，不存在则抛异常
   *
   * @param id 库位Id
   * @return 库位
   * @throws DomainException 库位不存在时抛出
   */
  private Location requireLocation(long id) {
    Location location = locationRepository.get(id);
    if (location == null) {
      throw new DomainException("库位" + id + "不存在!");
    }
    return location;
  }
}
