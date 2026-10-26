package com.rongia.production.domain.bomoperation;

import com.rongia.shared.domain.aggregate.UpdateAggregateRoot;
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
import org.jspecify.annotations.NullMarked;

/**
 * 工序行（BOM 或路线的子行，照 ERPNext 的 BOM Operation）
 *
 * <p>同一张表被两个父级共用：{@code ParentType} 区分挂在 BOM 上还是路线上，{@code ParentId} 指向那个父级的 Id。
 * 描述「这道工序在这个物料上怎么做」：顺序、工作中心、工时与批量。工序本身是什么由 {@code Operation} 定义，这里只引用它的 Id。
 *
 * <p>它是独立聚合而不是子表：一份 BOM 或一条路线改工序时，不必把整头连同全部工序读进来再整体保存。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "BomOperation",
    indexes = {
      @Index(name = "IX_BomOperation_Parent", columnList = "ParentType,ParentId"),
      @Index(name = "IX_BomOperation_OperationId", columnList = "OperationId"),
      @Index(name = "IX_BomOperation_WorkCenterId", columnList = "WorkCenterId"),
      @Index(name = "IX_BomOperation_Sequence", columnList = "Sequence")
      // 注意：ParentType + ParentId + Sequence 的过滤唯一索引带 WHERE IsDeleted=0，JPA 不支持，用 Flyway 创建
    })
public class BomOperation extends UpdateAggregateRoot {

  /** 父级类型编码，取值见 {@link BomOperationParentTypeEnum} */
  @Column(name = "ParentType", nullable = false, length = 50)
  private String parentType;

  /** 父级Id：按 ParentType 指向 Bom 或 Routing */
  @Column(name = "ParentId", nullable = false)
  private Long parentId;

  /** 工序Id */
  @Column(name = "OperationId", nullable = false)
  private Long operationId;

  /** 工作中心Id */
  @Column(name = "WorkCenterId", nullable = false)
  private Long workCenterId;

  /** 周期时间（分钟）：做一次加工循环要多久 */
  @Column(name = "CycleTime", nullable = false, precision = 18, scale = 2)
  private BigDecimal cycleTime;

  /** 批量大小（件）：一次投产多少件，0 表示不分批 */
  @Column(name = "BatchSize", nullable = false, precision = 18, scale = 2)
  private BigDecimal batchSize;

  /** 小时费率（元/小时，照 ERPNext 的 hour_rate）：建行时从工作中心带出来，之后可按这条行改 */
  @Column(name = "HourlyRate", nullable = false, precision = 18, scale = 2)
  private BigDecimal hourlyRate;

  /** 工序顺序 */
  @Column(name = "Sequence", nullable = false)
  private Integer sequence;

  /**
   * 构造器
   *
   * @param parentType 父级类型编码
   * @param parentId 父级Id
   * @param operationId 工序Id
   * @param workCenterId 工作中心Id
   * @param cycleTime 周期时间（分钟）
   * @param batchSize 批量大小（件）
   * @param hourlyRate 小时费率（元/小时）
   * @param sequence 工序顺序
   */
  BomOperation(
      String parentType,
      long parentId,
      long operationId,
      long workCenterId,
      BigDecimal cycleTime,
      BigDecimal batchSize,
      BigDecimal hourlyRate,
      int sequence) {
    apply(
        parentType,
        parentId,
        operationId,
        workCenterId,
        cycleTime,
        batchSize,
        hourlyRate,
        sequence);
  }

  /**
   * 修改工序行
   *
   * @param operationId 工序Id
   * @param workCenterId 工作中心Id
   * @param cycleTime 周期时间（分钟）
   * @param batchSize 批量大小（件）
   * @param hourlyRate 小时费率（元/小时）
   * @param sequence 工序顺序
   */
  void modify(
      long operationId,
      long workCenterId,
      BigDecimal cycleTime,
      BigDecimal batchSize,
      BigDecimal hourlyRate,
      int sequence) {
    apply(
        parentType,
        parentId,
        operationId,
        workCenterId,
        cycleTime,
        batchSize,
        hourlyRate,
        sequence);
  }

  // ==================== 私有：校验与赋值 ====================

  /**
   * 统一的校验与赋值入口（构造器与修改共用）
   *
   * <p>把「父级/主键/顺序校验 → 数值非负校验 → 赋值」收敛到一处，避免构造器和 {@link #modify} 各写一遍导致规则漂移。
   *
   * @param parentType 父级类型编码
   * @param parentId 父级Id
   * @param operationId 工序Id
   * @param workCenterId 工作中心Id
   * @param cycleTime 周期时间（分钟）
   * @param batchSize 批量大小（件）
   * @param hourlyRate 小时费率（元/小时）
   * @param sequence 工序顺序
   * @throws DomainException 父级、工序、工作中心、顺序、工时、批量或费率非法时抛出
   */
  private void apply(
      String parentType,
      long parentId,
      long operationId,
      long workCenterId,
      BigDecimal cycleTime,
      BigDecimal batchSize,
      BigDecimal hourlyRate,
      int sequence) {
    String validParentType =
        EnumUtils.getCode(BomOperationParentTypeEnum.class, parentType, "父级类型");
    if (parentId <= 0) {
      throw new DomainException("父级不能为空");
    }
    if (sequence <= 0) {
      throw new DomainException("工序顺序必须大于0");
    }
    if (operationId <= 0) {
      throw new DomainException("工序不能为空");
    }
    if (workCenterId <= 0) {
      throw new DomainException("工作中心不能为空");
    }
    validateNonNegative("周期时间", cycleTime);
    validateNonNegative("批量大小", batchSize);
    validateNonNegative("小时费率", hourlyRate);

    this.parentType = validParentType;
    this.parentId = parentId;
    this.operationId = operationId;
    this.workCenterId = workCenterId;
    this.cycleTime = cycleTime;
    this.batchSize = batchSize;
    this.hourlyRate = hourlyRate;
    this.sequence = sequence;
  }

  /**
   * 校验数值字段非负
   *
   * <p>参数类型在 {@code @NullMarked} 下即非空契约，调用方必须保证不为 {@code null}，因此这里只判负数。
   *
   * @param field 字段中文名，用于拼装错误提示
   * @param value 待校验的数值
   * @throws DomainException 值为负数时抛出
   */
  private void validateNonNegative(String field, BigDecimal value) {
    if (value.signum() < 0) {
      throw new DomainException(field + "不能为负数");
    }
  }
}
