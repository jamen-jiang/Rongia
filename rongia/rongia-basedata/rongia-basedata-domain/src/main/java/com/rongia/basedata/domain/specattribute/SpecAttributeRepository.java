package com.rongia.basedata.domain.specattribute;

import com.rongia.shared.domain.repository.Repository;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 规格属性仓储接口 */
@NullMarked
public interface SpecAttributeRepository extends Repository<SpecAttribute> {

  /**
   * 按分类 + 代码查询
   *
   * @param categoryId 所属分类Id
   * @param code 规格属性代码
   * @return 规格属性（可空），不存在返回 null
   */
  @Nullable SpecAttribute getByCategoryIdAndCode(long categoryId, String code);

  /**
   * 按分类查询列表
   *
   * @param categoryId 所属分类Id
   * @return 规格属性列表
   */
  List<SpecAttribute> getListByCategoryId(long categoryId);

  /**
   * 判断规格属性代码在分类下是否已存在
   *
   * @param categoryId 所属分类Id
   * @param code 规格属性代码
   * @param id 排除的规格属性Id（可空）
   * @return true 表示已存在
   */
  boolean isExistByCategoryIdAndCodeExcludeId(long categoryId, String code, @Nullable Long id);

  /**
   * 判断规格属性代码在分类下是否已存在
   *
   * @param categoryId 所属分类Id
   * @param code 规格属性代码
   * @return true 表示已存在
   */
  default boolean isExistByCategoryIdAndCodeExcludeId(long categoryId, String code) {
    return isExistByCategoryIdAndCodeExcludeId(categoryId, code, null);
  }

  /**
   * 判断这批分类下面是否配着规格属性
   *
   * <p>与物料同一道删除保护：分类被删掉后这些规格属性就成了没有归属的定义，所以应用层删分类前要一起回查这一批Id。
   *
   * @param categoryIds 分类Id集合（一个分类连同子孙）
   * @return true 表示至少有一个分类配着规格属性
   */
  boolean isExistByCategoryIds(List<Long> categoryIds);
}
