package com.rongia.basedata.domain.suppliermaterial;

import com.rongia.shared.domain.exception.DomainException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 供应商物料工厂，负责创建供应商卡 */
@NullMarked
public class SupplierMaterialFactory {

  /** 供应商物料仓储 */
  private final SupplierMaterialRepository supplierMaterialRepository;

  /**
   * 构造器注入
   *
   * @param supplierMaterialRepository 供应商物料仓储
   */
  public SupplierMaterialFactory(SupplierMaterialRepository supplierMaterialRepository) {
    this.supplierMaterialRepository = supplierMaterialRepository;
  }

  /**
   * 创建供应商卡
   *
   * @param supplierId 供应商Id
   * @param materialId 物料系列Id
   * @param supplierMaterialCode 供应商料号（可空）
   * @param price 采购价格
   * @param discount 折扣（%，可空）
   * @param currencyId 币种Id
   * @param moq 起订量（可空）
   * @param leadTime 货期（天，可空）
   * @param effectiveFrom 生效时间
   * @param effectiveTo 失效时间（可空）
   * @return 供应商卡聚合
   * @throws DomainException 同键同生效时间已有卡时抛出
   */
  public SupplierMaterial create(
      long supplierId,
      long materialId,
      @Nullable String supplierMaterialCode,
      BigDecimal price,
      @Nullable BigDecimal discount,
      long currencyId,
      @Nullable BigDecimal moq,
      @Nullable Integer leadTime,
      LocalDateTime effectiveFrom,
      @Nullable LocalDateTime effectiveTo) {
    // 同键（供应商 + 物料）的生效时间不能撞车，否则取价会出现两张都合法
    if (supplierMaterialRepository.isExistBySupplierIdAndMaterialIdAndEffectiveFromExcludeId(
        supplierId, materialId, effectiveFrom)) {
      throw new DomainException("该供应商这个物料在此生效时间的价格已存在!");
    }
    return new SupplierMaterial(
        supplierId,
        materialId,
        supplierMaterialCode,
        price,
        discount,
        currencyId,
        moq,
        leadTime,
        effectiveFrom,
        effectiveTo);
  }
}
