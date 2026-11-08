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
 * <p>一行说的是「哪个 SKU、多少，从哪个存货点流到哪个存货点」：出库仓库是货离开的存货点，入库仓库是货到达的存货点。单子上没有
 * 「入库」「出库」这一说——出库仓库填了就在出库仓库扣，入库仓库填了就往入库仓库加（ERPNext 的过账口径：出库仓库减、入库仓库加），
 * 领料只填出库仓库、完工入库只填入库仓库、调拨两个都填，方向是填仓库填出来的结果。两个仓库至少填一个：都不填等于货没
 * 地方来也没地方去。SPU 只作快照，给按系列汇总和打印用。单位不抄——数量按 SKU 基本单位计这一条
 * {@code Stock} 已经定了，再抄一份单位就多一个说法。
 *
 * <p>数量带符号，红字行取负。数量只说这笔账往回走多少，正负不表示方向。负数只由红冲产生，所以「草稿里的负数行」
 * 等于「这张单是红字单」。
 *
 * <p>红字行上记着被冲的那一原行的Id（{@code reversalOfStockEntryLineId}）：红冲挑原单上的行开红字行，
 * 一次只冲一部分、同一原行冲好几回，靠它把每行冲掉多少认到原行头上，不用拿位置去猜。
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

  /** 出库仓库Id（可空，货从这里离开；与入库仓库至少填一个） */
  @Column(name = "OutboundWarehouseId")
  @Nullable
  private Long outboundWarehouseId;

  /** 入库仓库Id（可空，货往这里到达；与出库仓库至少填一个） */
  @Column(name = "InboundWarehouseId")
  @Nullable
  private Long inboundWarehouseId;

  /** 来源单据行Id（可空，指回业务单上的那一行：工单用料行、销售单行、采购单行） */
  @Column(name = "SourceLineId")
  @Nullable
  private Long sourceLineId;

  /** 被冲的原行Id（可空，非空即本行是红字行、指回原单上被冲的那一行） */
  @Column(name = "ReversalOfStockEntryLineId")
  @Nullable
  private Long reversalOfStockEntryLineId;

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
   * @param outboundWarehouseId 出库仓库Id（可空）
   * @param inboundWarehouseId 入库仓库Id（可空）
   * @param sourceLineId 来源单据行Id（可空）
   * @param reversalOfStockEntryLineId 被冲的原行Id（可空，普通行为 null）
   * @param remark 备注（可空）
   * @throws DomainException SPU、SKU非法，数量为零，出库仓库与入库仓库全空或相同，或原行Id有误时抛出
   */
  StockEntryLine(
      long materialId,
      long variantId,
      BigDecimal qty,
      @Nullable Long outboundWarehouseId,
      @Nullable Long inboundWarehouseId,
      @Nullable Long sourceLineId,
      @Nullable Long reversalOfStockEntryLineId,
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
    if (outboundWarehouseId != null && outboundWarehouseId <= 0) {
      throw new DomainException("出库仓库Id有误!");
    }
    if (inboundWarehouseId != null && inboundWarehouseId <= 0) {
      throw new DomainException("入库仓库Id有误!");
    }
    if (outboundWarehouseId == null && inboundWarehouseId == null) {
      throw new DomainException("出库仓库与入库仓库至少填一个");
    }
    if (outboundWarehouseId != null && outboundWarehouseId.equals(inboundWarehouseId)) {
      throw new DomainException("出库仓库与入库仓库不能相同");
    }
    if (reversalOfStockEntryLineId != null && reversalOfStockEntryLineId <= 0) {
      throw new DomainException("红字行要指回被冲的原行!");
    }
    if (sourceLineId != null && sourceLineId <= 0) {
      throw new DomainException("来源行Id有误!");
    }
    this.materialId = materialId;
    this.variantId = variantId;
    this.qty = qty;
    this.outboundWarehouseId = outboundWarehouseId;
    this.inboundWarehouseId = inboundWarehouseId;
    this.sourceLineId = sourceLineId;
    this.reversalOfStockEntryLineId = reversalOfStockEntryLineId;
    this.remark = remark;
  }
}
