package com.rongia.production.domain.workorder;

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
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 工单用料行
 *
 * <p>工单下达时由 BOM 展开生成：需求量按 BOM 行的单位用量、父件数量与损耗率算出来，SPU 一并快照，
 * 之后 BOM 改版不影响本单。投放工序也从 BOM 行照抄（可空）；物料类型不在这里抄：它记在 {@code Variant}
 * 主数据上，要按类别看料就按 SPU 分组。
 *
 * <p>四个数量记录料的完整流转：要多少（required）、领多少（issued）、退多少（returned）、
 * 用多少（consumed）。差异即超领、退料或盘亏。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "WorkOrderLine",
    indexes = {
      @Index(name = "IX_WorkOrderLine_WorkOrderId", columnList = "WorkOrderId"),
      @Index(name = "IX_WorkOrderLine_MaterialId", columnList = "MaterialId"),
      @Index(name = "IX_WorkOrderLine_VariantId", columnList = "VariantId")
    })
public class WorkOrderLine extends BaseDeleteEntity {

  /** 工单Id */
  @Column(name = "WorkOrderId")
  private Long workOrderId;

  /** 来源BOM行Id（可空，手工加料时为空） */
  @Column(name = "BomLineId")
  @Nullable
  private Long bomLineId;

  /** 物料SPU */
  @Column(name = "MaterialId", nullable = false)
  private Long materialId;

  /** 物料SKU */
  @Column(name = "VariantId", nullable = false)
  private Long variantId;

  /**
   * 投放路线工序Id（可空，下达时从 BOM 行照抄）：这颗料在哪道工序干活时被用掉。
   *
   * <p>割边开工发磨削液、镀膜开工发膜料，靠它对上；空表示从头跟着走，开工就把料备到车间。
   */
  @Column(name = "RoutingOperationId")
  @Nullable
  private Long routingOperationId;

  /** 需求数量 */
  @Column(name = "RequiredQty", nullable = false, precision = 18, scale = 2)
  private BigDecimal requiredQty;

  /** 已领数量 */
  @Column(name = "IssuedQty", nullable = false, precision = 18, scale = 2)
  private BigDecimal issuedQty;

  /** 已耗数量 */
  @Column(name = "ConsumedQty", nullable = false, precision = 18, scale = 2)
  private BigDecimal consumedQty;

  /** 已退数量 */
  @Column(name = "ReturnedQty", nullable = false, precision = 18, scale = 2)
  private BigDecimal returnedQty;

  /** 单位Id */
  @Column(name = "UnitId", nullable = false)
  private Long unitId;

  /**
   * 构造器
   *
   * @param bomLineId 来源BOM行Id（可空，手工加料时为空）
   * @param materialId 物料SPU
   * @param variantId 物料SKU
   * @param routingOperationId 投放路线工序Id（可空，下达时从 BOM 行照抄）
   * @param requiredQty 需求数量
   * @param unitId 单位Id
   * @throws DomainException SPU、物料、单位非法或需求数量为负时抛出
   */
  WorkOrderLine(
      @Nullable Long bomLineId,
      long materialId,
      long variantId,
      @Nullable Long routingOperationId,
      BigDecimal requiredQty,
      long unitId) {
    if (materialId <= 0) {
      throw new DomainException("物料SPU不能为空");
    }
    if (variantId <= 0) {
      throw new DomainException("物料不能为空");
    }
    if (requiredQty.signum() < 0) {
      throw new DomainException("需求数量不能为负数");
    }
    if (unitId <= 0) {
      throw new DomainException("单位不能为空");
    }
    this.bomLineId = bomLineId;
    this.materialId = materialId;
    this.variantId = variantId;
    this.routingOperationId = routingOperationId;
    this.requiredQty = requiredQty;
    this.issuedQty = BigDecimal.ZERO;
    this.consumedQty = BigDecimal.ZERO;
    this.returnedQty = BigDecimal.ZERO;
    this.unitId = unitId;
  }

  /**
   * 领料：累加已领数量
   *
   * <p>上限按净领料算（已领减已退再加本次不超过需求）：退回去的料等于把额度让出来，返工补领时不该被自己
   * 先前退的那一笔挡住。超领要放行就得回去把需求量调大，而不是在这里放宽校验。
   *
   * @param qty 本次领用数量
   * @throws DomainException 数量非正、或净领料累计超过需求数量时抛出
   */
  public void issue(BigDecimal qty) {
    if (qty.signum() <= 0) {
      throw new DomainException("领料数量必须大于0");
    }
    BigDecimal netIssued = this.issuedQty.subtract(this.returnedQty).add(qty);
    if (netIssued.compareTo(this.requiredQty) > 0) {
      throw new DomainException(
          "物料"
              + this.variantId
              + "超领：需求"
              + this.requiredQty
              + "，净领料将达"
              + netIssued);
    }
    this.issuedQty = this.issuedQty.add(qty);
  }

  /**
   * 退料：累加已退数量
   *
   * <p>退料只能退「已领但还没耗用」的部分，否则等于凭空多出一批料。
   *
   * @param qty 本次退料数量
   * @throws DomainException 数量非正、或退料超过已领未耗数量时抛出
   */
  public void returnMaterial(BigDecimal qty) {
    if (qty.signum() <= 0) {
      throw new DomainException("退料数量必须大于0");
    }
    if (qty.compareTo(remaining()) > 0) {
      throw new DomainException("退料数量超过已领未耗数量");
    }
    this.returnedQty = this.returnedQty.add(qty);
  }

  /**
   * 撤回领料：把已领数量减回去
   *
   * <p>只能撤「已领、还没退也没耗」的那部分：料一旦进了工序就追不回来，硬减会让已耗超过已领，净领料与
   * 需求量的对账也就散了。
   *
   * @param qty 本次撤回数量
   * @throws DomainException 数量非正、或撤回超过可撤回的数量时抛出
   */
  public void unissue(BigDecimal qty) {
    if (qty.signum() <= 0) {
      throw new DomainException("撤回领料数量必须大于0");
    }
    if (qty.compareTo(remaining()) > 0) {
      throw new DomainException("物料" + this.variantId + "撤回数量超过已领未耗数量" + remaining());
    }
    this.issuedQty = this.issuedQty.subtract(qty);
  }

  /**
   * 撤回退料：把已退数量减回去
   *
   * <p>退料单作废或红冲等于「这批料没退回仓库」，所以只减已退；已领数量当初照批时加过，退料没动它，这里也不动。
   * 减完要照净领料上限重查一遍：退料把额度让出去之后往往又补领过，这一减就把超领放回来了。
   *
   * @param qty 本次撤回数量
   * @throws DomainException 数量非正、撤回超过已退数量、或撤回后净领料超过需求量时抛出
   */
  public void unreturn(BigDecimal qty) {
    if (qty.signum() <= 0) {
      throw new DomainException("撤回退料数量必须大于0");
    }
    BigDecimal left = this.returnedQty.subtract(qty);
    if (left.signum() < 0) {
      throw new DomainException("物料" + this.variantId + "撤回数量超过已退数量" + this.returnedQty);
    }
    BigDecimal netIssued = this.issuedQty.subtract(left);
    if (netIssued.compareTo(this.requiredQty) > 0) {
      throw new DomainException(
          "物料"
              + this.variantId
              + "超领：需求"
              + this.requiredQty
              + "，撤回退料后净领料将达"
              + netIssued);
    }
    this.returnedQty = left;
  }

  /**
   * 耗用：累加已耗数量
   *
   * @param qty 本次耗用数量
   * @throws DomainException 数量为负、或累计耗用超过已领未退数量时抛出
   */
  public void consume(BigDecimal qty) {
    if (qty.signum() < 0) {
      throw new DomainException("耗用数量不能为负数");
    }
    BigDecimal next = this.consumedQty.add(qty);
    if (next.compareTo(this.issuedQty.subtract(this.returnedQty)) > 0) {
      throw new DomainException("累计耗用数量超过已领未退数量");
    }
    this.consumedQty = next;
  }

  /**
   * 已领未耗数量
   *
   * @return 已领数量减已退数量再减已耗数量
   */
  public BigDecimal remaining() {
    return this.issuedQty.subtract(this.returnedQty).subtract(this.consumedQty);
  }
}
