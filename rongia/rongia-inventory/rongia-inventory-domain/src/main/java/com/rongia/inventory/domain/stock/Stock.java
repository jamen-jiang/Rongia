package com.rongia.inventory.domain.stock;

import com.rongia.shared.domain.aggregate.UpdateAggregateRoot;
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

/**
 * 库存（一个SKU在一个仓库上的在库快照）
 *
 * <p>一行回答的是「这个SKU，在这个仓库（可能再加一个库位），现在有多少」：定位键是 {@code VariantId +
 * WarehouseId}，数量用SKU基本单位计，不另存单位列。{@code MaterialId} 是SKU所属的SPU，只给「按系列汇总
 * 有多少货」这类查询当分组列用，不参与定位，也不是键的一部分。
 *
 * <p>两个数量各说一件事：{@code Qty} 是「架上现在有多少」，{@code ReservedQty} 是「其中已经答应给
 * 某张单、还没发出的有多少」。后者是**派生的合计**——名记在领料行自己身上（批了多少、发没发），这边只是把
 * 没发完的加起来，为的是别每次查可用量都去翻所有未过账的领料单；随时能按领料行重算核对，对不上就是回写漏了。
 * 金蝶的即时库存锁库量、Odoo 的 {@code stock.quant.reserved_qty}、ERPNext 的 {@code Bin.reserved_qty}
 * 都是这种"存着的派生数"。可用量不存，{@link #availableQty()} 现算。
 *
 * <p>「这批货能不能动用」不靠库存分档表达：D365 把库存状态当存储维度组里的一维，SAP 在 MARD 上让非限制/
 * 质检/冻结各占一列，Odoo 用 QC 区、Blocked 区这类库位表达——要么多一种状态就改一次表和程序，要么让物理
 * 位置和会计语义挤在同一个字段上。这里一概不取：待检、冻结这类处置还没落地，等有单据承载时再定形态。
 *
 * <p>数量只能通过入库、出库、预留、取消预留四个动作变动，没有直接改数的入口：库存要能被对账，就必须保证
 * 每次变动都有明确的业务动作对应。键的唯一性由应用层保证（先查后插），不在表上建唯一索引兜底，一键一行由
 * {@code StockDomainService} 先查后建保证。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "Stock",
    indexes = {
      @Index(name = "IX_Stock_VariantId_WarehouseId", columnList = "VariantId,WarehouseId")
    })
public class Stock extends UpdateAggregateRoot {

  /** SPU（汇总维度，不参与定位键） */
  @Column(name = "MaterialId", nullable = false)
  private Long materialId;

  /** SKU */
  @Column(name = "VariantId", nullable = false)
  private Long variantId;

  /** 仓库Id */
  @Column(name = "WarehouseId", nullable = false)
  private Long warehouseId;

  /** 在库数量（SKU基本单位） */
  @Column(name = "Qty", nullable = false, precision = 18, scale = 2)
  private BigDecimal qty;

  /** 预留数量（已答应给某张需求单、还没发出的部分，是未发完需求量的合计） */
  @Column(name = "ReservedQty", nullable = false, precision = 18, scale = 2)
  private BigDecimal reservedQty;

  /**
   * 构造器，新建的行数量为零，之后由入库动作累加
   *
   * @param materialId SKU所属SPU（汇总维度）
   * @param variantId SKU
   * @param warehouseId 仓库Id
   * @throws DomainException SPU或SKU或仓库不合法时抛出
   */
  Stock(long materialId, long variantId, long warehouseId) {
    if (materialId <= 0) {
      throw new DomainException("SPU不能为空");
    }
    if (variantId <= 0) {
      throw new DomainException("SKU不能为空");
    }
    if (warehouseId <= 0) {
      throw new DomainException("仓库不能为空");
    }
    this.materialId = materialId;
    this.variantId = variantId;
    this.warehouseId = warehouseId;
    this.qty = BigDecimal.ZERO;
    this.reservedQty = BigDecimal.ZERO;
  }

  // ==================== 数量变动 ====================

  /**
   * 入库：在库数量增加（采购收货、完工入库、盘盈都走这里）
   *
   * @param qty 入库数量，必须为正数
   * @throws DomainException 数量不为正数时抛出
   */
  public void inbound(BigDecimal qty) {
    validatePositive("入库数量", qty);
    this.qty = this.qty.add(qty);
  }

  /**
   * 出库：在库数量减少
   *
   * <p>闸是两道：先判架上够不够（缺货），再判可用量够不够（抢单）——可用量 = 在库 − 已答应别人的部分。
   * 例：架上 100，工单批了 30 还没发，别人来领 90 就当场抛错，而不是等发完才发现那 30 是空的。
   *
   * <p>要领自己那张单批下来的货，**先 {@link #unreserve} 把这笔预留退掉、再出库**，别在出库里顺手核销：
   * 库存行只有一个预留合计，不知道这 30 是谁占的，出库自动扣就会扣到别人头上（BC 的预留登记、SAP 的
   * {@code RESB} 提领、Odoo 让 move line 自己带 {@code reserved_qty}，都把"释放占用"当成出库前的独立一步）。
   *
   * @param qty 出库数量，必须为正数
   * @throws DomainException 出库量不为正数，或在库 / 可用不足时抛出
   */
  public void outbound(BigDecimal qty) {
    validatePositive("出库数量", qty);
    validateEnough(qty);
    this.qty = this.qty.subtract(qty);
  }

  /**
   * 预留：把可用量占住（领料单批准、出库单分配时调）
   *
   * @param qty 预留数量，必须为正数
   * @throws DomainException 数量不为正数、或可用量不足时抛出
   */
  public void reserve(BigDecimal qty) {
    validatePositive("预留数量", qty);
    if (availableQty().compareTo(qty) < 0) {
      throw new DomainException("可用库存不足，当前" + availableQty() + "，不能预留" + qty);
    }
    this.reservedQty = this.reservedQty.add(qty);
  }

  /**
   * 取消预留：把先前答应的数量放回可用量（发料过账、单据作废、批准量调小时调）
   *
   * @param qty 取消预留数量，必须为正数
   * @throws DomainException 数量不为正数、或超过已预留时抛出
   */
  public void unreserve(BigDecimal qty) {
    validatePositive("取消预留数量", qty);
    if (this.reservedQty.compareTo(qty) < 0) {
      throw new DomainException(
          "预留数量不足，当前" + this.reservedQty + "，不能取消预留" + qty);
    }
    this.reservedQty = this.reservedQty.subtract(qty);
  }

  // ==================== 查询 ====================

  /**
   * 可用量：在库数量减预留数量
   *
   * <p>这是「还能被预留或领走」的量。要知道全仓能动多少，就按仓库把各行加起来。
   *
   * @return 还能被预留或领用的数量
   */
  public BigDecimal availableQty() {
    return qty.subtract(reservedQty);
  }

  // ==================== 私有：校验 ====================

  /**
   * 校验架上和可用量够不够发
   *
   * <p>两种不足分开报：架上不够是缺货，架上够但已答应别人是抢单，处理办法完全不同。
   *
   * @param qty 出库数量
   * @throws DomainException 任何一种不够时抛出
   */
  private void validateEnough(BigDecimal qty) {
    if (this.qty.compareTo(qty) < 0) {
      throw new DomainException("在库数量不足，当前" + this.qty + "，不能出库" + qty);
    }
    BigDecimal available = availableQty();
    if (qty.compareTo(available) > 0) {
      throw new DomainException(
          "可用数量不足，当前"
              + available
              + "（在库"
              + this.qty
              + "，其中"
              + this.reservedQty
              + "已答应给别的单据还没发），不能出库"
              + qty);
    }
  }

  /**
   * 校验变动数量必须为正数
   *
   * <p>入库与出库各自决定加还是减，入参只允许正数，避免「传个负数做出库」把方向搞混。
   *
   * @param field 数量字段中文名，用于拼装错误提示
   * @param value 待校验的数量
   * @throws DomainException 数量不为正数时抛出
   */
  private void validatePositive(String field, BigDecimal value) {
    if (value.signum() <= 0) {
      throw new DomainException(field + "必须大于零");
    }
  }
}
