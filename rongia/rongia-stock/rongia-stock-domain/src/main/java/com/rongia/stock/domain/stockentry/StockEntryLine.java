package com.rongia.stock.domain.stockentry;

import com.rongia.shared.domain.entity.BaseDeleteEntity;
import com.rongia.shared.domain.exception.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 库存单据行
 *
 * <p>一行说的是「哪个 SKU、多少，从哪个仓流到哪个仓」：出库库位是货离开的仓，入库库位是货到达的仓。单子上没有
 * 「入库」「出库」这一说——出库库位填了就在出库库位扣，入库库位填了就往入库库位加（ERPNext 的过账口径：出库库位减、入库库位加），
 * 领料只填出库库位、完工入库只填入库库位、调拨两个都填，方向是填仓填出来的结果。两个仓至少填一个：都不填等于货没
 * 地方来也没地方去。SPU 只作快照，给按系列汇总和打印用。单位不抄——数量按 SKU 基本单位计这一条
 * {@code Stock} 已经定了，再抄一份单位就多一个说法。
 *
 * <p>数量带符号，红字行取负。数量只说这笔账往回走多少，正负不表示方向。负数只由红冲产生，所以「草稿里的负数行」
 * 等于「这张单是红字单」。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "StockEntryLine",
    indexes = {
      @Index(name = "IX_StockEntryLine_StockEntryId", columnList = "StockEntryId"),
      @Index(name = "IX_StockEntryLine_VariantId", columnList = "VariantId")
    })
public class StockEntryLine extends BaseDeleteEntity {

  /** 库存单据Id */
  @Column(name = "StockEntryId")
  private Long stockEntryId;

  /** 物料SPU（快照，仅作汇总与展示） */
  @Column(name = "MaterialId", nullable = false)
  private Long materialId;

  /** 物料SKU */
  @Column(name = "VariantId", nullable = false)
  private Long variantId;

  /** 数量（SKU基本单位；红字行为负） */
  @Column(name = "Qty", nullable = false, precision = 18, scale = 2)
  private BigDecimal qty;

  /** 出库库位Id（可空，货从这里离开；与入库库位至少填一个） */
  @Column(name = "OutboundLocationId")
  @Nullable
  private Long outboundLocationId;

  /** 入库库位Id（可空，货往这里到达；与出库库位至少填一个） */
  @Column(name = "InboundLocationId")
  @Nullable
  private Long inboundLocationId;

  /** 备注（可空） */
  @Nationalized
  @Column(name = "Remark", length = 500)
  @Nullable
  private String remark;

  /**
   * 构造器
   *
   * @param materialId 物料SPU（快照）
   * @param variantId 物料SKU
   * @param qty 数量（红字行为负）
   * @param outboundLocationId 出库库位Id（可空）
   * @param inboundLocationId 入库库位Id（可空）
   * @param remark 备注（可空）
   * @throws DomainException SPU、SKU非法，数量为零，出库库位与入库库位全空，或出库库位与入库库位相同时抛出
   */
  StockEntryLine(
      long materialId,
      long variantId,
      BigDecimal qty,
      @Nullable Long outboundLocationId,
      @Nullable Long inboundLocationId,
      @Nullable String remark) {
    if (materialId <= 0) {
      throw new DomainException("物料SPU不能为空");
    }
    if (variantId <= 0) {
      throw new DomainException("物料不能为空");
    }
    if (qty.signum() == 0) {
      throw new DomainException("数量不能为零");
    }
    if (outboundLocationId != null && outboundLocationId <= 0) {
      throw new DomainException("出库库位Id有误!");
    }
    if (inboundLocationId != null && inboundLocationId <= 0) {
      throw new DomainException("入库库位Id有误!");
    }
    if (outboundLocationId == null && inboundLocationId == null) {
      throw new DomainException("出库库位库与入库库位库至少填一个：货没地方来也没地方去，这笔账记不成");
    }
    if (outboundLocationId != null && outboundLocationId.equals(inboundLocationId)) {
      throw new DomainException("出库库位库与入库库位库相同，这一行是在原地倒腾");
    }
    this.materialId = materialId;
    this.variantId = variantId;
    this.qty = qty;
    this.outboundLocationId = outboundLocationId;
    this.inboundLocationId = inboundLocationId;
    this.remark = remark;
  }
}
