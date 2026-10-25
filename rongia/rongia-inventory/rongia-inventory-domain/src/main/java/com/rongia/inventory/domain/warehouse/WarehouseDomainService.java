package com.rongia.inventory.domain.warehouse;

import com.rongia.shared.domain.exception.DomainException;
import com.rongia.shared.domain.util.HierarchyUtils;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 仓库领域服务，负责跨聚合编排与树形维护
 *
 * <p>仓库是树之后，改父仓要连带把所有子孙的层级路径重算，并拦住「父仓挂到子仓下」的环；
 * 这套编排跟 Category 是同一个套路，收在本服务里。
 */
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
   * 导入生成：仓库存在则修改，不存在则创建
   *
   * @param parentId 上级仓库Id（可空）
   * @param code 代码
   * @param name 名称
   * @param isLeaf 是否末级
   * @param disabled 是否停用
   * @param remark 备注（可空）
   * @return 仓库聚合
   */
  public Warehouse importGenerate(
      @Nullable Long parentId,
      String code,
      String name,
      boolean isLeaf,
            boolean disabled,
      @Nullable String remark) {
    Warehouse model = warehouseRepository.getByCode(code);
    if (model == null) {
      return create(parentId, code, name, isLeaf, disabled, remark);
    }
    modify(model, parentId, code, name, isLeaf, disabled, remark);
    return model;
  }

  /**
   * 创建仓库
   *
   * @param parentId 上级仓库Id（可空）
   * @param code 代码
   * @param name 名称
   * @param isLeaf 是否末级
   * @param disabled 是否停用
   * @param remark 备注（可空）
   * @return 仓库聚合
   * @throws DomainException 上级仓库不存在、或上级是末级仓时抛出
   */
  public Warehouse create(
      @Nullable Long parentId,
      String code,
      String name,
      boolean isLeaf,
            boolean disabled,
      @Nullable String remark) {
    Warehouse parent = parentId == null ? null : requireWarehouse(parentId);
    if (parent != null && Boolean.TRUE.equals(parent.getIsLeaf())) {
      throw new DomainException("末级仓库" + parent.getName() + "下面不能建子仓!");
    }
    return warehouseFactory.create(
        parentId,
        HierarchyUtils.resolveParentPath(parentId, parent, "上级仓库"),
        code,
        name,
        isLeaf,
        disabled,
        remark);
  }

  /**
   * 修改仓库（按Id查询后修改）
   *
   * @param id 仓库Id
   * @param parentId 上级仓库Id（可空）
   * @param code 代码
   * @param name 名称
   * @param isLeaf 是否末级
   * @param disabled 是否停用
   * @param remark 备注（可空）
   * @return 仓库聚合
   * @throws DomainException 仓库不存在、代码冲突、或层级关系成环时抛出
   */
  public Warehouse modify(
      long id,
      @Nullable Long parentId,
      String code,
      String name,
      boolean isLeaf,
            boolean disabled,
      @Nullable String remark) {
    Warehouse model = warehouseRepository.get(id);
    if (model == null) {
      throw new DomainException("仓库" + id + "不存在!");
    }
    modify(model, parentId, code, name, isLeaf, disabled, remark);
    return model;
  }

  /**
   * 修改仓库（使用已有聚合）
   *
   * @param model 仓库聚合
   * @param parentId 上级仓库Id（可空）
   * @param code 代码
   * @param name 名称
   * @param isLeaf 是否末级
   * @param disabled 是否停用
   * @param remark 备注（可空）
   * @throws DomainException 仓库代码冲突、或层级关系成环时抛出
   */
  public void modify(
      Warehouse model,
      @Nullable Long parentId,
      String code,
      String name,
      boolean isLeaf,
            boolean disabled,
      @Nullable String remark) {
    if (warehouseRepository.isExistByCodeExcludeId(code, model.getId())) {
      throw new DomainException("仓库代码" + code + "已存在!");
    }
    Warehouse parent = parentId == null ? null : requireWarehouse(parentId);
    if (parent != null) {
      if (Objects.equals(parent, model)) {
        throw new DomainException("仓库的上级仓库不能是自己");
      }
      if (isDescendant(model, parent)) {
        throw new DomainException("上级仓库" + parent.getCode() + "是本仓的下级，不能反过来挂");
      }
    }
    model.modify(
        parentId,
        HierarchyUtils.resolveParentPath(parentId, parent, "上级仓库"),
        code,
        name,
        isLeaf,
        disabled,
        remark);
    refreshDescendants(model);
  }

  /**
   * 删除仓库（逻辑删除）
   *
   * @param id 仓库Id
   * @return 仓库聚合（已标记删除）
   * @throws DomainException 仓库不存在、或下面还有子仓时抛出
   */
  public Warehouse delete(long id) {
    Warehouse model = warehouseRepository.get(id);
    if (model == null) {
      throw new DomainException("仓库" + id + "不存在!");
    }
    if (!warehouseRepository.getListByParentId(model.getId()).isEmpty()) {
      throw new DomainException("仓库" + model.getCode() + "下面还有子仓，不能删除");
    }
    model.delete();
    return model;
  }

  /**
   * 查一个仓下面的直接子仓
   *
   * @param parentId 上级仓库Id
   * @return 子仓列表
   */
  public List<Warehouse> getChildren(long parentId) {
    return warehouseRepository.getListByParentId(parentId);
  }

  // ==================== 私有：树维护与校验 ====================

  /**
   * 递归刷新下级仓库的层级路径
   *
   * @param model 已刷新过自身数据的仓库
   */
  private void refreshDescendants(Warehouse model) {
    List<Warehouse> children = warehouseRepository.getListByParentId(model.getId());
    if (children.isEmpty()) {
      return;
    }
    HierarchyUtils.resetChildrenParentPath(model, children);
    for (Warehouse child : children) {
      refreshDescendants(child);
    }
  }

  /**
   * 判断 candidate 是不是 unit 的子孙
   *
   * @param unit 本仓库
   * @param candidate 待检查的仓库（新上级）
   * @return true 表示 candidate 在 unit 下面
   */
  private boolean isDescendant(Warehouse unit, Warehouse candidate) {
    Warehouse current = candidate;
    int depth = 0;
    final int maxDepth = 50;
    while (current != null && depth < maxDepth) {
      if (Objects.equals(current, unit)) {
        return true;
      }
      current =
          current.getParentId() == null ? null : warehouseRepository.get(current.getParentId());
      depth++;
    }
    return false;
  }

  /**
   * 取仓库，不存在则抛异常
   *
   * @param id 仓库Id
   * @return 仓库
   * @throws DomainException 仓库不存在时抛出
   */
  private Warehouse requireWarehouse(long id) {
    Warehouse warehouse = warehouseRepository.get(id);
    if (warehouse == null) {
      throw new DomainException("仓库" + id + "不存在!");
    }
    return warehouse;
  }
}
