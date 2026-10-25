package com.rongia.basedata.domain.category;

import com.rongia.shared.domain.repository.Repository;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 物料分类仓储接口 */
@NullMarked
public interface CategoryRepository extends Repository<Category> {

  /**
   * 根据代码查询分类
   *
   * @param code 分类代码
   * @return 分类实体（可空），不存在返回 null
   */
  @Nullable Category getByCode(String code);

  /**
   * 查询指定分类及其所有子孙分类（包含自身）
   *
   * @param id 分类Id
   * @return 分类及其子孙列表
   */
  List<Category> getWithChildren(long id);

  /**
   * 查询指定父级下的直接子分类
   *
   * @param parentId 父级Id
   * @return 子分类列表
   */
  List<Category> getListByParentId(long parentId);

  /**
   * 判断分类代码是否已存在
   *
   * @param code 分类代码
   * @param id 排除的Id（修改时传自身Id，可为空）
   * @return 是否存在
   */
  boolean isExistByCodeExcludeId(String code, @Nullable Long id);

  /**
   * 判断分类代码是否已存在（新增时校验，无需排除Id）
   *
   * @param code 分类代码
   * @return 是否存在
   */
  default boolean isExistByCodeExcludeId(String code) {
    return isExistByCodeExcludeId(code, null);
  }

  /**
   * 判断同一父级下分类名称是否已存在
   *
   * @param name 分类名称
   * @param parentId 父级Id（可空）
   * @param id 排除的Id（修改时传自身Id，可为空）
   * @return 是否存在
   */
  boolean isExistByNameAndParentIdExcludeId(
      String name, @Nullable Long parentId, @Nullable Long id);

  /**
   * 判断同一父级下分类名称是否已存在（新增时校验，无需排除Id）
   *
   * @param name 分类名称
   * @param parentId 父级Id（可空）
   * @return 是否存在
   */
  default boolean isExistByNameAndParentIdExcludeId(String name, @Nullable Long parentId) {
    return isExistByNameAndParentIdExcludeId(name, parentId, null);
  }
}
