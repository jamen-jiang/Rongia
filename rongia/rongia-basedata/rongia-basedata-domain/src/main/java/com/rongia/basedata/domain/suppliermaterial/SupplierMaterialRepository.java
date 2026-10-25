package com.rongia.basedata.domain.suppliermaterial;

import com.rongia.shared.domain.repository.Repository;
import java.time.LocalDateTime;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 供应商物料仓储接口 */
@NullMarked
public interface SupplierMaterialRepository extends Repository<SupplierMaterial> {

  /**
   * 按供应商 + 物料系列查询全部供应商卡
   *
   * @param supplierId 供应商Id
   * @param materialId 物料系列Id
   * @return 供应商卡列表
   */
  List<SupplierMaterial> getListBySupplierIdAndMaterialId(long supplierId, long materialId);

  /**
   * 判断同键同生效时间是否已有供应商卡
   *
   * @param supplierId 供应商Id
   * @param materialId 物料系列Id
   * @param effectiveFrom 生效时间
   * @param id 排除的供应商卡Id（可空，新增时传 null）
   * @return true 表示已存在
   */
  boolean isExistBySupplierIdAndMaterialIdAndEffectiveFromExcludeId(
      long supplierId, long materialId, LocalDateTime effectiveFrom, @Nullable Long id);

  /**
   * 判断同键同生效时间是否已有供应商卡
   *
   * @param supplierId 供应商Id
   * @param materialId 物料系列Id
   * @param effectiveFrom 生效时间
   * @return true 表示已存在
   */
  default boolean isExistBySupplierIdAndMaterialIdAndEffectiveFromExcludeId(
      long supplierId, long materialId, LocalDateTime effectiveFrom) {
    return isExistBySupplierIdAndMaterialIdAndEffectiveFromExcludeId(
        supplierId, materialId, effectiveFrom, null);
  }
}
