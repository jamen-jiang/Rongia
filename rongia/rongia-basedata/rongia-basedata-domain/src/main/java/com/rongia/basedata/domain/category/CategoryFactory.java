package com.rongia.basedata.domain.category;

import com.rongia.shared.domain.exception.DomainException;
import com.rongia.shared.domain.util.HierarchyUtils;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 物料分类工厂，负责创建分类 */
@NullMarked
public class CategoryFactory {

  /** 分类仓储 */
  private final CategoryRepository categoryRepository;

  /**
   * 构造器注入
   *
   * @param categoryRepository 分类仓储
   */
  public CategoryFactory(CategoryRepository categoryRepository) {
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
   * @throws DomainException 分类代码已存在时抛出
   * @throws DomainException 同级分类名称已存在时抛出
   * @throws DomainException 父分类不存在、或父分类是末级分类时抛出
   */
  public Category create(
      @Nullable Long parentId,
      String code,
      String name,
      boolean isLeaf,
      boolean disabled,
      @Nullable Integer sequence,
      @Nullable String remark) {
    // 校验分类代码全局唯一
    if (categoryRepository.isExistByCodeExcludeId(code)) {
      throw new DomainException("分类代码" + code + "已存在!");
    }
    // 校验同级分类名称唯一
    if (categoryRepository.isExistByNameAndParentIdExcludeId(name, parentId)) {
      throw new DomainException("同级分类名称" + name + "已存在!");
    }
    Category parent = getParent(parentId);
    // 末级分类是物料的落点，下面不能再开子分类，否则汇总会出现父类自有物料与子类物料混算
    if (parent != null && parent.getIsLeaf()) {
      throw new DomainException("末级分类" + parent.getName() + "下面不能建子分类!");
    }
    return new Category(
        parentId,
        HierarchyUtils.resolveParentPath(parentId, parent, "父分类"),
        code,
        name,
        isLeaf,
        sequence,
        disabled,
        remark);
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
}
