package com.rongia.production.domain.bom;

import com.rongia.shared.domain.repository.Repository;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** BOM 仓储接口 */
@NullMarked
public interface BomRepository extends Repository<Bom> {

  /**
   * 按父件SPU查询全部 BOM（含各版本与已停用）
   *
   * @param materialId 父件SPU
   * @return BOM 列表
   */
  List<Bom> getListByMaterialId(long materialId);

  /**
   * 按父件SPU + SKU + 版本号查询
   *
   * @param materialId 父件SPU
   * @param variantId 父件SKU（可空，为空表示系列共用）
   * @param version 版本号
   * @return BOM（可空），不存在返回 null
   */
  @Nullable Bom getByMaterialIdAndVariantIdAndVersion(
      long materialId, @Nullable Long variantId, String version);

  /**
   * 取父件未禁用的 BOM
   *
   * <p>先找该变体专用的，找不到再找系列共用的（{@code variantId} 为空）；同一层级多条未禁用时按 Id 倒序取最新一条。
   *
   * @param materialId 父件SPU
   * @param variantId 父件SKU（可空，为空表示只找系列共用的）
   * @return BOM（可空），没有未禁用的 BOM 时返回 null
   */
  @Nullable Bom getEnabledByVariantId(long materialId, @Nullable Long variantId);

  /**
   * 判断父件SPU + SKU + 版本号是否存在
   *
   * @param materialId 父件SPU
   * @param variantId 父件SKU（可空）
   * @param version 版本号
   * @param id 排除的 BOM Id（可空，新增时传 null）
   * @return true 表示已存在
   */
  boolean isExistByMaterialIdAndVariantIdAndVersionExcludeId(
      long materialId, @Nullable Long variantId, String version, @Nullable Long id);

  /**
   * 判断父件SPU + SKU + 版本号是否存在
   *
   * @param materialId 父件SPU
   * @param variantId 父件SKU（可空）
   * @param version 版本号
   * @return true 表示已存在
   */
  default boolean isExistByMaterialIdAndVariantIdAndVersionExcludeId(
      long materialId, @Nullable Long variantId, String version) {
    return isExistByMaterialIdAndVariantIdAndVersionExcludeId(
        materialId, variantId, version, null);
  }
}
