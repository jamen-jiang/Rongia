package com.rongia.basedata.domain.variant;

import com.rongia.shared.domain.repository.Repository;
import java.util.Collection;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** SKU 仓储 */
@NullMarked
public interface VariantRepository extends Repository<Variant> {
  /**
   * 按Id列表查询
   *
   * @param ids Id列表
   * @return Variant列表
   */
  List<Variant> getListByIds(Collection<Long> ids);

  /**
   * 按规格编码查询
   *
   * @param code 规格编码（物料代码 + 规格属性组合）
   * @return SKU（可空），不存在返回 null
   */
  @Nullable Variant getByCode(String code);

  /**
   * 按 MaterialId 查询列表
   *
   * @param materialId MaterialId
   * @return SKU 列表
   */
  List<Variant> getListByMaterialId(long materialId);

  /**
   * 判断规格编码是否存在
   *
   * @param code 规格编码
   * @return true：存在
   */
  boolean isExistByCode(String code);
}
