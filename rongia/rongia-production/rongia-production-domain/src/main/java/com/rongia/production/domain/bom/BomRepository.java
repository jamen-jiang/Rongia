package com.rongia.production.domain.bom;

import com.rongia.shared.domain.repository.Repository;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** BOM 仓储接口 */
@NullMarked
public interface BomRepository extends Repository<Bom> {

  /**
   * 按父件SPU查询全部 BOM（含已停用）
   *
   * @param materialId 父件SPU
   * @return BOM 列表
   */
  List<Bom> getListByMaterialId(long materialId);

  /**
   * 取父件未禁用的 BOM
   *
   * <p>优先取 {@code isDefault} 为 true 且未禁用的；同一层级没有默认的，先找变体专用、再找系列共用的
   * （{@code variantId} 为空），多条时按 Id 倒序取最新一条。
   *
   * @param materialId 父件SPU
   * @param variantId 父件SKU（可空，为空表示只找系列共用的）
   * @return BOM（可空），没有未禁用的 BOM 时返回 null
   */
  @Nullable Bom getEnabledByVariantId(long materialId, @Nullable Long variantId);
}
