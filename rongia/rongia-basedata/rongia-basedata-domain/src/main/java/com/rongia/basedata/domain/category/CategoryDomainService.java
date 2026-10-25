package com.rongia.basedata.domain.category;

import com.rongia.shared.domain.exception.DomainException;
import com.rongia.shared.domain.util.HierarchyUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 物料分类领域服务，负责层级编排
 *
 * <p>是否被物料、规格属性引用而不允许删除，不在这里判断——本项目的领域服务只编排自己聚合内的规则，
 * 跨聚合的引用校验由应用层在删除前完成。
 */
@NullMarked
public class CategoryDomainService {

  /** 分类工厂 */
  private final CategoryFactory categoryFactory;

  /** 分类仓储 */
  private final CategoryRepository categoryRepository;

  /**
   * 构造器注入
   *
   * @param categoryFactory 分类工厂
   * @param categoryRepository 分类仓储
   */
  public CategoryDomainService(
      CategoryFactory categoryFactory, CategoryRepository categoryRepository) {
    this.categoryFactory = categoryFactory;
    this.categoryRepository = categoryRepository;
  }

  /**
   * 创建分类
   *
   * @param parentId 父分类Id（可空）
   * @param code 分类代码
   * @param name 分类名称
   * @param isLeaf 是否末级
   * @param disabled 是否禁用
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   * @return 分类聚合
   * @throws DomainException 分类代码或同级名称已存在、父分类不存在、或父分类是末级分类时抛出
   */
  public Category create(
      @Nullable Long parentId,
      String code,
      String name,
      boolean isLeaf,
      boolean disabled,
      @Nullable Integer sequence,
      @Nullable String remark) {
    return categoryFactory.create(parentId, code, name, isLeaf, disabled, sequence, remark);
  }

  /**
   * 修改分类（含层级编排）
   *
   * @param id 分类Id
   * @param parentId 父分类Id（可空）
   * @param code 分类代码
   * @param name 分类名称
   * @param isLeaf 是否末级
   * @param disabled 是否禁用
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   * @return 需要持久化的聚合列表（自身 + 受影响的子孙）
   * @throws DomainException 分类代码或同级名称已存在、分类不存在时抛出
   * @throws DomainException 把分类挂到自己或自己的子孙下面、父分类是末级分类时抛出
   * @throws DomainException 这一类下面还有子分类、却想改成末级时抛出
   */
  public List<Category> modify(
      long id,
      @Nullable Long parentId,
      String code,
      String name,
      boolean isLeaf,
      boolean disabled,
      @Nullable Integer sequence,
      @Nullable String remark) {
    Category model = getExist(id);
    // 校验分类代码唯一（排除自身）
    if (categoryRepository.isExistByCodeExcludeId(code, id)) {
      throw new DomainException("分类代码" + code + "已存在!");
    }
    // 校验同级分类名称唯一（排除自身）
    if (categoryRepository.isExistByNameAndParentIdExcludeId(name, parentId, id)) {
      throw new DomainException("同级分类名称" + name + "已存在!");
    }
    validateParent(model, parentId);
    Category parent = getParent(parentId);
    if (parent != null && parent.getIsLeaf()) {
      throw new DomainException("末级分类" + parent.getName() + "下面不能建子分类!");
    }
    // 末级是物料的落点：下面挂着子分类的不能改成末级，否则两批物料会混在同一个汇总数里
    if (!isLeaf && !categoryRepository.getListByParentId(model.getId()).isEmpty()) {
      throw new DomainException("分类" + model.getName() + "下面还有子分类，不能改成末级!");
    }
    model.modify(
        parentId,
        HierarchyUtils.resolveParentPath(parentId, parent, "父分类"),
        code,
        name,
        isLeaf,
        sequence,
        disabled,
        remark);
    List<Category> list = new ArrayList<>();
    setChildrenParentPath(model, list);
    list.addFirst(model);
    return list;
  }

  /**
   * 校验这个分类能挂物料：必须存在且是末级
   *
   * <p>物料建档与修改都要过这一关，跨聚合的引用校验归应用层，但"这一类能不能落物料"是分类自己的规则，
   * 所以留在这里，由应用层在保存物料前调用。
   *
   * @param categoryId 分类Id
   * @return 分类聚合
   * @throws DomainException 分类不存在、或不是末级分类时抛出
   */
  public Category ensureLeaf(long categoryId) {
    Category model = getExist(categoryId);
    if (!model.getIsLeaf()) {
      throw new DomainException("分类" + model.getName() + "是分组节点，物料只能挂在末级分类下!");
    }
    return model;
  }

  /**
   * 删除自身及所有子孙分类
   *
   * @param id 分类Id
   * @return 需要持久化的聚合列表（已标记删除）
   * @throws DomainException 分类不存在时抛出
   */
  public List<Category> removeWithChildren(long id) {
    Category model = getExist(id);
    List<Category> list = categoryRepository.getWithChildren(model.getId());
    for (Category item : list) {
      item.delete();
    }
    return list;
  }

  // ==================== 私有：取数与校验 ====================

  /**
   * 校验新父节点合法：不能是自己，也不能是自己的子孙
   *
   * <p>否则层级路径会自我包含，树变成环，之后按路径查子孙会无限套。
   *
   * @param model 当前分类
   * @param parentId 父分类Id（可空）
   * @throws DomainException 父节点是自己或自己的子孙时抛出
   */
  private void validateParent(Category model, @Nullable Long parentId) {
    if (parentId == null) {
      return;
    }
    if (Objects.equals(parentId, model.getId())) {
      throw new DomainException("父分类不能是自己!");
    }
    boolean isDescendant =
        categoryRepository.getWithChildren(model.getId()).stream()
            .anyMatch(item -> Objects.equals(item.getId(), parentId));
    if (isDescendant) {
      throw new DomainException("父分类不能是自己的子孙分类!");
    }
  }

  /**
   * 取分类，不存在则抛异常
   *
   * @param id 分类Id
   * @return 分类聚合
   * @throws DomainException 分类不存在时抛出
   */
  private Category getExist(long id) {
    Category model = categoryRepository.get(id);
    if (model == null) {
      throw new DomainException("分类" + id + "不存在!");
    }
    return model;
  }

  /**
   * 查询父分类
   *
   * @param parentId 父分类Id（可空）
   * @return 父分类，无父Id或不存在时返回 null（可空）
   */
  @Nullable
  private Category getParent(@Nullable Long parentId) {
    return parentId == null ? null : categoryRepository.get(parentId);
  }

  /**
   * 递归重设子孙分类的层级路径
   *
   * @param model 分类
   * @param list 子孙收集列表
   */
  private void setChildrenParentPath(Category model, List<Category> list) {
    if (model.getId() == 0) {
      return;
    }
    List<Category> children = categoryRepository.getListByParentId(model.getId());
    HierarchyUtils.resetChildrenParentPath(model, children);
    for (Category child : children) {
      setChildrenParentPath(child, list);
      list.add(child);
    }
  }
}
