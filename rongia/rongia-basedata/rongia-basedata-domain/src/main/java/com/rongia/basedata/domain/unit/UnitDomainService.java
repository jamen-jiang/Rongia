package com.rongia.basedata.domain.unit;

import com.rongia.shared.domain.exception.DomainException;
import com.rongia.shared.domain.util.HierarchyUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 单位领域服务，负责跨聚合编排
 *
 * <p>树形的维护都收在这里：改一个单位的参考单位或换算值，要连带把它下面所有子孙的绝对系数与层级路径重算；
 * 换算前要确认两个单位在同一棵树里（同一个根），否则「米换千克」会被当成 1000 换 1 算出来。
 */
@NullMarked
public class UnitDomainService {

  /** 中间计算保留的小数位 */
  private static final int CALC_SCALE = 12;

  /** 往上找根的深度上限，防脏数据把父链接成环 */
  private static final int MAX_DEPTH = 50;

  /** 单位工厂 */
  private final UnitFactory unitFactory;

  /** 单位仓储 */
  private final UnitRepository unitRepository;

  /**
   * 构造器注入
   *
   * @param unitFactory 单位工厂
   * @param unitRepository 单位仓储
   */
  public UnitDomainService(UnitFactory unitFactory, UnitRepository unitRepository) {
    this.unitFactory = unitFactory;
    this.unitRepository = unitRepository;
  }

  /**
   * 新建单位
   *
   * @param parentId 参考单位Id（可空，空表示根）
   * @param code 代码
   * @param name 名称
   * @param relativeFactor 包含几个参考单位
   * @param rounding 数量落数步长
   * @param disabled 是否禁用
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   * @return 单位聚合
   * @throws DomainException 参考单位不存在、或单位代码已存在时抛出
   */
  public Unit create(
      @Nullable Long parentId,
      String code,
      String name,
      BigDecimal relativeFactor,
      BigDecimal rounding,
      boolean disabled,
      @Nullable Integer sequence,
      @Nullable String remark) {
    Unit parent = parentId == null ? null : requireUnit(parentId);
    return unitFactory.create(
        parentId,
        code,
        name,
        HierarchyUtils.resolveParentPath(parentId, parent, "参考单位"),
        relativeFactor,
        parent == null ? null : parent.getFactor(),
        rounding,
        disabled,
        sequence,
        remark);
  }

  /**
   * 修改单位
   *
   * <p>换参考单位时先查环：新的参考单位不能是自己，也不能是自己的子孙。改完把子孙的系数与层级路径一并重算。
   *
   * @param id 单位Id
   * @param parentId 参考单位Id（可空，空表示根）
   * @param code 代码
   * @param name 名称
   * @param relativeFactor 包含几个参考单位
   * @param rounding 数量落数步长
   * @param disabled 是否禁用
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   * @return 单位聚合
   * @throws DomainException 单位不存在、代码重复、参考单位成环时抛出
   */
  public Unit modify(
      long id,
      @Nullable Long parentId,
      String code,
      String name,
      BigDecimal relativeFactor,
      BigDecimal rounding,
      boolean disabled,
      @Nullable Integer sequence,
      @Nullable String remark) {
    Unit model = requireUnit(id);
    Unit parent = parentId == null ? null : requireUnit(parentId);
    if (parent != null) {
      if (Objects.equals(parent, model)) {
        throw new DomainException("单位的参考单位不能是自己");
      }
      if (isDescendant(model, parent)) {
        throw new DomainException("参考单位" + parent.getCode() + "是本单位的下级，不能反过来挂");
      }
    }
    model.modify(
        parentId,
        code,
        name,
        HierarchyUtils.resolveParentPath(parentId, parent, "参考单位"),
        relativeFactor,
        parent == null ? null : parent.getFactor(),
        rounding,
        disabled,
        sequence,
        remark);
    refreshDescendants(model);
    return model;
  }

  /**
   * 删除单位（逻辑删除）
   *
   * <p>下面还挂着单位的不许删：删了它们就找不到根，换算与显示都会断。
   *
   * @param id 单位Id
   * @return 单位聚合（已标记删除）
   * @throws DomainException 单位不存在、或还有下级单位时抛出
   */
  public Unit delete(long id) {
    Unit model = requireUnit(id);
    if (!unitRepository.getListByParentId(model.getId()).isEmpty()) {
      throw new DomainException("单位" + model.getCode() + "下面还有下级单位，不能删除");
    }
    model.delete();
    return model;
  }

  /**
   * 把数量从一个单位换算成另一个单位
   *
   * <p>{@code 数量 × 来源系数 ÷ 目标系数}，最后按**目标单位**的步长落数。两个单位必须同根，跨树直接报错。
   *
   * @param fromUnitId 来源单位Id
   * @param toUnitId 目标单位Id
   * @param qty 原数量
   * @return 换算后的数量
   * @throws DomainException 单位不存在、不同树、数量为负时抛出
   */
  public BigDecimal convert(long fromUnitId, long toUnitId, BigDecimal qty) {
    Unit from = requireUnit(fromUnitId);
    Unit to = requireUnit(toUnitId);
    if (qty.signum() < 0) {
      throw new DomainException("换算数量不能是负数，当前" + qty);
    }
    if (fromUnitId == toUnitId) {
      return qty;
    }
    if (!Objects.equals(rootOf(from), rootOf(to))) {
      throw new DomainException(
          "单位" + from.getCode() + "与" + to.getCode() + "不在同一棵单位树上，不能换算");
    }
    BigDecimal amount =
        qty
            .multiply(from.getFactor())
            .divide(to.getFactor(), CALC_SCALE, RoundingMode.HALF_UP);
    return to.roundQty(amount);
  }

  /**
   * 查一个单位下面的直接下级
   *
   * @param parentId 参考单位Id
   * @return 下级单位列表
   */
  public List<Unit> getChildren(long parentId) {
    return unitRepository.getListByParentId(parentId);
  }

  // ==================== 私有：树维护与校验 ====================

  /**
   * 递归刷新下级单位的层级路径与绝对系数
   *
   * @param model 已刷新过自身数据的单位
   */
  private void refreshDescendants(Unit model) {
    List<Unit> children = unitRepository.getListByParentId(model.getId());
    if (children.isEmpty()) {
      return;
    }
    HierarchyUtils.resetChildrenParentPath(model, children);
    for (Unit child : children) {
      child.recalculate(model.getFactor());
      refreshDescendants(child);
    }
  }

  /**
   * 判断 candidate 是不是 unit 的子孙
   *
   * @param unit 本单位
   * @param candidate 待检查的单位（新参考单位）
   * @return true 表示 candidate 在 unit 下面
   */
  private boolean isDescendant(Unit unit, Unit candidate) {
    Unit current = candidate;
    int depth = 0;
    while (current != null && depth < MAX_DEPTH) {
      if (Objects.equals(current, unit)) {
        return true;
      }
      current = current.getParentId() == null ? null : unitRepository.get(current.getParentId());
      depth++;
    }
    return false;
  }

  /**
   * 沿父链找到根
   *
   * @param unit 单位
   * @return 根单位
   * @throws DomainException 父链超过深度上限（说明数据成环）时抛出
   */
  private Unit rootOf(Unit unit) {
    Unit current = unit;
    int depth = 0;
    while (current.getParentId() != null) {
      if (depth++ > MAX_DEPTH) {
        throw new DomainException("单位" + unit.getCode() + "的参考单位链成环了!");
      }
      Unit parent = unitRepository.get(current.getParentId());
      if (parent == null) {
        throw new DomainException("单位" + current.getCode() + "的参考单位不存在!");
      }
      current = parent;
    }
    return current;
  }

  /**
   * 取单位，不存在则抛异常
   *
   * @param id 单位Id
   * @return 单位
   * @throws DomainException 单位不存在时抛出
   */
  private Unit requireUnit(long id) {
    Unit unit = unitRepository.get(id);
    if (unit == null) {
      throw new DomainException("单位" + id + "不存在!");
    }
    return unit;
  }
}
