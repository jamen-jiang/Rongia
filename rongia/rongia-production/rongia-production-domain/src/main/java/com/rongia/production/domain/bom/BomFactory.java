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
   * @param version 版本号
   * @param materialId 父件SPU
   * @param variantId 父件SKU（可空）
   * @param routingId 工艺路线Id（可空）
   * @param qty 产出数量
   * @param unitId 产出单位Id
   * @param startMode 开工方式编码
   * @param issueMode 领料方式编码
   * @param leadDays 制造天数（天）
   * @param prepareDays 提前备料天数
   * @param batchSize 自动生成工单的批量（可空）
   * @param disabled 是否禁用
   * @param remark 备注（可空）
   * @return BOM 聚合
   * @throws DomainException 父件SKU + 版本已存在时抛出
   */
  public Bom create(
      String type,
      @Nullable String code,
      String version,
      long materialId,
      @Nullable Long variantId,
      @Nullable Long routingId,
      BigDecimal qty,
      long unitId,
      String startMode,
      String issueMode,
      int leadDays,
      int prepareDays,
      @Nullable BigDecimal batchSize,
      boolean disabled,
      @Nullable String remark) {
    // 校验父件SPU + SKU + 版本唯一
    if (bomRepository.isExistByMaterialIdAndVariantIdAndVersionExcludeId(
        materialId, variantId, version)) {
      throw new DomainException("该父件的 BOM 版本" + version + "已存在!");
    }
    return new Bom(
        type,
        code,
        version,
        materialId,
        variantId,
        routingId,
        qty,
        unitId,
        startMode,
        issueMode,
        leadDays,
        prepareDays,
        batchSize,
        disabled,
        remark);
  }
}
