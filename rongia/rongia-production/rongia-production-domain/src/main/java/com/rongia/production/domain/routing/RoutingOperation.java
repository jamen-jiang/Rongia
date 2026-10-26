package com.rongia.production.domain.routing;

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

/**
 * 路线工序（工艺路线上的一道工序）
 *
 * <p>描述「这道工序在这条路线上怎么做」：顺序、工作中心、工时与批量。工序本身是什么由 {@code Operation}
 * 定义，这里只引用它的 Id，并在建行时把工序字典与工作中心上的默认值抄一份——之后路线自己说了算，
 * 工序字典改默认值不影响已有路线。
 *
 * <p>它是路线的子行：一份配方要做不同工序，就配一条不同的路线；BOM 通过 {@code routingId} 挑路线，
 * 用料行通过 {@code routingOperationId} 指到「消耗在哪道工序」。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "RoutingOperation",
    indexes = {
      @Index(name = "IX_RoutingOperation_RoutingId", columnList = "RoutingId"),
      @Index(name = "IX_RoutingOperation_OperationId", columnList = "OperationId"),
      @Index(name = "IX_RoutingOperation_WorkCenterId", columnList = "WorkCenterId"),
      @Index(name = "IX_RoutingOperation_Sequence", columnList = "Sequence")
      // 注意：RoutingId + Sequence 的过滤唯一索引带 WHERE IsDeleted=0，JPA 不支持，用 Flyway 创建
    })
public class RoutingOperation extends BaseDeleteEntity {

  /** 工艺路线Id */
  @Column(name = "RoutingId")
  private Long routingId;

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

  /** 小时费率（元/小时）：建行时从工作中心带出来，之后可按这条行改 */
  @Column(name = "HourlyRate", nullable = false, precision = 18, scale = 2)
  private BigDecimal hourlyRate;

  /** 工序顺序 */
  @Column(name = "Sequence", nullable = false)
  private Integer sequence;

  /**
   * 构造器
   *
   * @param operationId 工序Id
   * @param workCenterId 工作中心Id
   * @param cycleTime 周期时间（分钟）
   * @param batchSize 批量大小（件）
   * @param hourlyRate 小时费率（元/小时）
   * @param sequence 工序顺序
   */
  RoutingOperation(
      long operationId,
      long workCenterId,
      BigDecimal cycleTime,
      BigDecimal batchSize,
      BigDecimal hourlyRate,
      int sequence) {
    apply(operationId, workCenterId, cycleTime, batchSize, hourlyRate, sequence);
  }

  /**
   * 修改路线工序
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
    apply(operationId, workCenterId, cycleTime, batchSize, hourlyRate, sequence);
  }

  // ==================== 私有：校验与赋值 ====================

  /**
   * 统一的校验与赋值入口（构造器与修改共用）
   *
   * @param operationId 工序Id
   * @param workCenterId 工作中心Id
   * @param cycleTime 周期时间（分钟）
   * @param batchSize 批量大小（件）
   * @param hourlyRate 小时费率（元/小时）
   * @param sequence 工序顺序
   * @throws DomainException 工序、工作中心、顺序、工时、批量或费率非法时抛出
   */
  private void apply(
      long operationId,
      long workCenterId,
      BigDecimal cycleTime,
      BigDecimal batchSize,
      BigDecimal hourlyRate,
      int sequence) {
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
