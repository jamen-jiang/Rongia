package com.rongia.basedata.domain.material;

import com.rongia.shared.domain.repository.Repository;
import java.util.Collection;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 物料仓储接口 */
@NullMarked
public interface MaterialRepository extends Repository<Material> {
  /**
   * 按Id集合批量查询
   *
   * @param ids 主键集合
   * @return 物料列表
   */
  List<Material> getByIds(Collection<Long> ids);

  /**
   * 按物料代码查询
   *
   * @param code 物料代码
   * @return 物料（可空），不存在返回 null
   */
  @Nullable Material getByCode(String code);

  /**
   * 判断物料代码是否存在
   *
   * @param code 物料代码
   * @param id 排除的主键Id（可空），修改时排除自身
   * @return true 表示已存在
   */
  boolean isExistByCodeExcludeId(String code, @Nullable Long id);

  /**
   * 判断物料代码是否存在
   *
   * @param code 物料代码
   * @return true 表示已存在
   */
  default boolean isExistByCodeExcludeId(String code) {
    return this.isExistByCodeExcludeId(code, null);
  }

  /**
   * 判断这批分类下面是否挂着物料
   *
   * <p>删除分类会连带删掉它整棵子树，应用层删之前要拿这一批Id来回查：物料还挂着就不能删。这个引用只有物料侧数得出来，
   * 分类聚合自己不知道下面有多少物料。
   *
   * @param categoryIds 分类Id集合（一个分类连同子孙）
   * @return true 表示至少有一个分类挂着物料
   */
  boolean isExistByCategoryIds(List<Long> categoryIds);
}
