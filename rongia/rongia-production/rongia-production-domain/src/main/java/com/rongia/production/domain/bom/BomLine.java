package com.rongia.production.domain.bom;

import com.rongia.shared.domain.entity.BaseDeleteEntity;
import com.rongia.shared.domain.exception.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * BOM 行（用料明细）
 *
 * <p>一行只描述「生产一份 BOM 产出批量需要多少子件」——单位用量对应的是表头的
 * {@code qty}，不是 1 个父件。真正的需求量在工单下达时按「工单数量 ÷ 产出数量」与损耗率算出来，
 * 存到 {@code WorkOrderLine}，不在这里保存任何订单相关数据。
 *
 * <p>{@code materialId} 是子件所属的 SPU，由调用方随子件一起提交，只当「按系列查哪些 BOM 用到了它」的分组列用。
 * 物料类型不在这里存：它记在 {@code Variant} 主数据上；「子件是不是自制件」也不靠类型标签判定，而是看这个子件
 * 自己在生产侧有没有 BOM，那是本上下文里能直接问出的事实。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "BomLine",
    indexes = {
      @Index(name = "IX_BomLine_BomId", columnList = "BomId"),
      @Index(name = "IX_BomLine_MaterialId", columnList = "MaterialId"),
      @Index(name = "IX_BomLine_VariantId", columnList = "VariantId"),
      @Index(name = "IX_BomLine_Sequence", columnList = "Sequence")
      // 注意：BomId + VariantId 的过滤唯一索引带 WHERE IsDeleted=0，JPA 不支持，用 Flyway 创建
    })
public class BomLine extends BaseDeleteEntity {

  /** BOM Id */
  @Column(name = "BomId")
  private Long bomId;

  /** 子件SPU（按系列查哪些 BOM 用到了它） */
  @Column(name = "MaterialId", nullable = false)
  private Long materialId;

  /** 子件SKU */
  @Column(name = "VariantId", nullable = false)
  private Long variantId;

  /** 消耗在哪道工序行（可空，直接指 RoutingOperation 那一行） */
  @Column(name = "RoutingOperationId")
  @Nullable
  private Long routingOperationId;

  /** 单位用量（生产 1 个父件消耗的子件数量） */
  @Column(name = "Qty", nullable = false, precision = 18, scale = 2)
  private BigDecimal qty;

  /** 损耗率（百分比） */
  @Column(name = "LossRate", nullable = false, precision = 9, scale = 2)
  private BigDecimal lossRate;

  /** 单位Id */
  @Column(name = "UnitId", nullable = false)
  private Long unitId;

  /** 行号 */
  @Column(name = "Sequence", nullable = false)
  private Integer sequence;

  /**
   * 构造器
   *
   * @param materialId 子件SPU
   * @param variantId 子件SKU
   * @param routingOperationId 消耗的路线工序Id（可空）
   * @param qty 单位用量
   * @param lossRate 损耗率（百分比）
   * @param unitId 单位Id
   * @param sequence 行号
   */
  BomLine(
      long materialId,
      long variantId,
      @Nullable Long routingOperationId,
      BigDecimal qty,
      BigDecimal lossRate,
      long unitId,
      int sequence) {
    apply(materialId, variantId, routingOperationId, qty, lossRate, unitId, sequence);
  }

  /**
   * 修改 BOM 行
   *
   * @param materialId 子件SPU
   * @param variantId 子件SKU
   * @param routingOperationId 消耗的路线工序Id（可空）
   * @param qty 单位用量
   * @param lossRate 损耗率（百分比）
   * @param unitId 单位Id
   * @param sequence 行号
   */
  void modify(
      long materialId,
      long variantId,
      @Nullable Long routingOperationId,
      BigDecimal qty,
      BigDecimal lossRate,
      long unitId,
      int sequence) {
    apply(materialId, variantId, routingOperationId, qty, lossRate, unitId, sequence);
  }

  /**
   * 按父件数量计算需求数量，保留 2 位小数
   *
   * <p>单位用量 × (父件数量 ÷ BOM 产出数量) × (1 + 损耗率 / 100)。单位用量是「BOM 产出数量」这一批量下的量，
   * 所以先折算成单件，再乘工单数量。
   *
   * @param parentQty 父件生产数量
   * @param outputQty BOM 的产出数量
   * @return 需求数量
   * @throws DomainException 父件数量为负、或产出数量非正时抛出
   */
  public BigDecimal requiredQty(BigDecimal parentQty, BigDecimal outputQty) {
    if (parentQty.signum() < 0) {
      throw new DomainException("父件数量不能为负数");
    }
    if (outputQty.signum() <= 0) {
      throw new DomainException("BOM 产出数量必须大于0");
    }
    return this.qty
        .multiply(parentQty)
        .divide(outputQty, 6, RoundingMode.HALF_UP)
        .multiply(lossFactor())
        .setScale(2, RoundingMode.HALF_UP);
  }

  // ==================== 私有：校验与赋值 ====================

  /**
   * 损耗系数：1 + 损耗率 / 100
   *
   * @return 损耗系数
   */
  private BigDecimal lossFactor() {
    BigDecimal rate = this.lossRate.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
    return BigDecimal.ONE.add(rate);
  }

  /**
   * 统一的校验与赋值入口（构造器与修改共用）
   *
   * @param materialId 子件SPU
   * @param variantId 子件SKU
   * @param routingOperationId 消耗的路线工序Id（可空）
   * @param qty 单位用量
   * @param lossRate 损耗率（百分比）
   * @param unitId 单位Id
   * @param sequence 行号
   * @throws DomainException 行号、SPU、子件、用量、单位或损耗率非法时抛出
   */
  private void apply(
      long materialId,
      long variantId,
      @Nullable Long routingOperationId,
      BigDecimal qty,
      BigDecimal lossRate,
      long unitId,
      int sequence) {
    if (sequence <= 0) {
      throw new DomainException("BOM 行号必须大于0");
    }
    if (materialId <= 0) {
      throw new DomainException("子件SPU不能为空");
    }
    if (variantId <= 0) {
      throw new DomainException("子件不能为空");
    }
    if (qty.signum() <= 0) {
      throw new DomainException("单位用量必须大于0");
    }
    if (unitId <= 0) {
      throw new DomainException("单位不能为空");
    }
    if (lossRate.signum() < 0) {
      throw new DomainException("损耗率不能为负数");
    }

    this.materialId = materialId;
    this.variantId = variantId;
    this.routingOperationId = routingOperationId;
    this.qty = qty;
    this.lossRate = lossRate;
    this.unitId = unitId;
    this.sequence = sequence;
  }
}
