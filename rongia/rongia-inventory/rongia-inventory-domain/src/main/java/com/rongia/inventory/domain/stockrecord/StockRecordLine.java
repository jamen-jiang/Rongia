package com.rongia.inventory.domain.stockrecord;

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
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Nationalized;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 库存记录行
 *
 * <p>一行说的是「哪个 SKU、在哪个位置、哪个库存状态、多少」：仓库 + 库位 + 库存状态正好对上库存表的一行，
 * 过账时就按它定位；货往哪个方向走由单据头定，整张单一个方向。SPU 只作快照，给按系列汇总和打印用。
 * 单位不抄——数量按 SKU 基本单位计这一条 {@code Stock} 已经定了，再抄一份单位就多一个说法。
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
    name = "StockRecordLine",
    indexes = {
      @Index(name = "IX_StockRecordLine_StockRecordId", columnList = "StockRecordId"),
      @Index(name = "IX_StockRecordLine_VariantId", columnList = "VariantId")
    })
public class StockRecordLine extends BaseDeleteEntity {

  /** 库存记录Id */
  @Column(name = "StockRecordId")
  private Long stockRecordId;

  /** 物料SPU（快照，仅作汇总与展示） */
  @Column(name = "MaterialId", nullable = false)
  private Long materialId;

  /** 物料SKU */
  @Column(name = "VariantId", nullable = false)
  private Long variantId;

  /** 数量（SKU基本单位；红字行为负） */
  @Column(name = "Qty", nullable = false, precision = 18, scale = 2)
  private BigDecimal qty;

  /** 仓库Id */
  @Column(name = "WarehouseId", nullable = false)
  private Long warehouseId;

  /** 库位Id（可空，空表示数量记到仓库这一层） */
  /** 库存状态编码（与仓库、库位一起定位库存行，见 {@code StockStatusEnum}） */
  @Nationalized
  @Column(name = "StockStatus", nullable = false, length = 50)
  private String stockStatus;

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
   * @param warehouseId 仓库Id
   * @param stockStatus 库存状态编码
   * @param remark 备注（可空）
   * @throws DomainException SPU、SKU、仓库、库位非法，库存状态为空，或数量为零时抛出
   */
  StockRecordLine(
      long materialId,
      long variantId,
      BigDecimal qty,
      long warehouseId,
      String stockStatus,
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
    if (warehouseId <= 0) {
      throw new DomainException("仓库不能为空");
    }
    String trimmedStatus = StringUtils.trimToNull(stockStatus);
    if (trimmedStatus == null) {
      throw new DomainException("库存状态不能为空");
    }
    this.materialId = materialId;
    this.variantId = variantId;
    this.qty = qty;
    this.warehouseId = warehouseId;
    this.stockStatus = trimmedStatus;
    this.remark = remark;
  }
}
