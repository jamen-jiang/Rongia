package com.rongia.production.domain.bom;

import com.rongia.shared.domain.exception.DomainException;
import java.math.BigDecimal;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** BOM 工厂，负责创建 BOM */
@NullMarked
public class BomFactory {

  /** BOM 仓储 */
  private final BomRepository bomRepository;

  /**
   * 构造器注入
   *
   * @param bomRepository BOM 仓储
   */
  public BomFactory(BomRepository bomRepository) {
    this.bomRepository = bomRepository;
  }

  /**
   * 创建 BOM
   *
   * @param type BOM 类型编码
   * @param code 参考号（可空）
   * @param materialId 父件SPU
   * @param variantId 父件SKU（可空）
   * @param routingId 工艺路线Id（可空）
   * @param qty 产出数量
   * @param unitId 产出单位Id
   * @param isDefault 是否默认（同一父件多张 BOM 时下达默认用哪张）
   * @param disabled 是否禁用
   * @param remark 备注（可空）
   * @return BOM 聚合
   */
  public Bom create(
      String type,
      @Nullable String code,
      long materialId,
      @Nullable Long variantId,
      @Nullable Long routingId,
      BigDecimal qty,
      long unitId,

      boolean isDefault,
      boolean disabled,
      @Nullable String remark) {
    return new Bom(
        type,
        code,
        materialId,
        variantId,
        routingId,
        qty,
        unitId,
        isDefault,
        disabled,
        remark);
  }
}
