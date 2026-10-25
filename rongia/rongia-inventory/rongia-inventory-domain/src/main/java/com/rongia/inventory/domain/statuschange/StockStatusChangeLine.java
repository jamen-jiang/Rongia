package com.rongia.inventory.domain.statuschange;

import com.rongia.inventory.domain.stock.StockStatusEnum;
import com.rongia.shared.domain.entity.BaseDeleteEntity;
import com.rongia.shared.domain.exception.DomainException;
import com.rongia.shared.domain.util.EnumUtils;
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
 * 库存状态转换单行（哪一格上的多少数量，从哪个状态挪到哪个状态）
 *
 * <p>一行说清四件事：哪个 SKU、在哪一格（仓库 + 库位）、从什么状态、到什么状态、挪多少。位置四个维度里，
 * 只有状态在变——库位不动、规格不动、总量不动，变的只是这批数量「能不能动用」。
 *
 * <p>方向不限定组合：待检 → 可用是放行，可用 → 冻结是扣住，冻结 → 可用是查清了放回，可用 → 废品是判定报废。
 * 域里不建「允许的转换路径」白名单，因为路径由各张来源单的性质决定（检验合格只能待检 → 可用），那是开单那一步
 * 的事，写死在这里就把处置的灵活性钉进库存。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "StockStatusChangeLine",
    indexes = {
      @Index(
          name = "IX_StockStatusChangeLine_StockStatusChangeId",
          columnList = "StockStatusChangeId"),
      @Index(name = "IX_StockStatusChangeLine_VariantId", columnList = "VariantId")
    })
public class StockStatusChangeLine extends BaseDeleteEntity {

  /** 转换单Id */
  @Column(name = "StockStatusChangeId")
  private Long stockStatusChangeId;

  /** 物料Id（快照，新建目标状态那行库存时要用） */
  @Column(name = "MaterialId", nullable = false)
  private Long materialId;

  /** 规格型号Id */
  @Column(name = "VariantId", nullable = false)
  private Long variantId;

  /** 仓库Id */
  @Column(name = "WarehouseId", nullable = false)
  private Long warehouseId;

  /** 库位Id（可空，空表示这一格的数量记在仓库这一层） */
  /** 转出状态编码，取值见 {@link StockStatusEnum} */
  @Nationalized
  @Column(name = "FromStockStatus", nullable = false, length = 50)
  private String fromStockStatus;

  /** 转入状态编码，取值见 {@link StockStatusEnum} */
  @Nationalized
  @Column(name = "ToStockStatus", nullable = false, length = 50)
  private String toStockStatus;

  /** 挪动数量（永远为正数，往回挪请开反向单） */
  @Column(name = "Qty", nullable = false, precision = 18, scale = 2)
  private BigDecimal qty;

  /** 备注（可空） */
  @Nationalized
  @Column(name = "Remark", length = 500)
  @Nullable
  private String remark;

  /**
   * 构造器
   *
   * @param materialId 物料Id（快照）
   * @param variantId 规格型号Id
   * @param warehouseId 仓库Id
   * @param fromStockStatus 转出状态编码
   * @param toStockStatus 转入状态编码
   * @param qty 挪动数量（正数）
   * @param remark 备注（可空）
   * @throws DomainException 定位维度非法、状态不在枚举内、转出与转入相同、或数量不为正数时抛出
   */
  StockStatusChangeLine(
      long materialId,
      long variantId,
      long warehouseId,
      String fromStockStatus,
      String toStockStatus,
      BigDecimal qty,
      @Nullable String remark) {
    if (materialId <= 0) {
      throw new DomainException("物料不能为空");
    }
    if (variantId <= 0) {
      throw new DomainException("规格型号不能为空");
    }
    if (warehouseId <= 0) {
      throw new DomainException("仓库不能为空");
    }
    String trimmedFrom = StringUtils.trimToNull(fromStockStatus);
    String trimmedTo = StringUtils.trimToNull(toStockStatus);
    this.fromStockStatus = EnumUtils.getCode(StockStatusEnum.class, fromStockStatus, "转出状态");
    this.toStockStatus = EnumUtils.getCode(StockStatusEnum.class, toStockStatus, "转入状态");
    if (trimmedFrom.equals(trimmedTo)) {
      throw new DomainException("转出状态与转入状态相同，这一行是在原地不动");
    }
    if (qty.signum() <= 0) {
      throw new DomainException("挪动数量必须大于零，往回挪请开一张反向的转换单");
    }

    this.materialId = materialId;
    this.variantId = variantId;
    this.warehouseId = warehouseId;
    this.qty = qty;
    this.remark = remark;
  }
}
