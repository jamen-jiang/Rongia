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
 * <p>描述「这道工序在这条路线上怎么做」：顺序、工作中心、三段工时。工序本身是什么由 {@code Operation}
 * 定义，这里只引用它的 Id；费率不在这里——它归工作中心。
 *
 * <p>三段工时各管一段成本：准备是每批一次（装夹调机），人工与机器按件计。分开记才能让三个费率各有乘数：
 * 人工工时乘人工费率、机器工时乘机器费率、总工时乘制造费用率。一炉多少件不在这里，那是设备的属性，
 * 记在工作中心的批次上限上。
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

  /** 准备时间（分钟，每批一次，照 carbon 的 setupTime）：装夹调机，与数量无关 */
  @Column(name = "SetupTime", nullable = false, precision = 18, scale = 2)
  private BigDecimal setupTime;

  /** 人工时间（分钟/件，照 carbon 的 laborTime）：一件要一个人干多久 */
  @Column(name = "LaborTime", nullable = false, precision = 18, scale = 2)
  private BigDecimal laborTime;

  /** 机器时间（分钟/件，照 carbon 的 machineTime）：一件要让机器转多久 */
  @Column(name = "MachineTime", nullable = false, precision = 18, scale = 2)
  private BigDecimal machineTime;

  /** 工序顺序 */
  @Column(name = "Sequence", nullable = false)
  private Integer sequence;

  /**
   * 构造器
   *
   * @param operationId 工序Id
   * @param workCenterId 工作中心Id
   * @param setupTime 准备时间（分钟，每批一次）
   * @param laborTime 人工时间（分钟/件）
   * @param machineTime 机器时间（分钟/件）
   * @param sequence 工序顺序
   */
  RoutingOperation(
      long operationId,
      long workCenterId,
      BigDecimal setupTime,
      BigDecimal laborTime,
      BigDecimal machineTime,
      int sequence) {
    apply(operationId, workCenterId, setupTime, laborTime, machineTime, sequence);
  }

  /**
   * 修改路线工序
   *
   * @param operationId 工序Id
   * @param workCenterId 工作中心Id
   * @param setupTime 准备时间（分钟，每批一次）
   * @param laborTime 人工时间（分钟/件）
   * @param machineTime 机器时间（分钟/件）
   * @param sequence 工序顺序
   */
  void modify(
      long operationId,
      long workCenterId,
      BigDecimal setupTime,
      BigDecimal laborTime,
      BigDecimal machineTime,
      int sequence) {
    apply(operationId, workCenterId, setupTime, laborTime, machineTime, sequence);
  }

  // ==================== 私有：校验与赋值 ====================

  /**
   * 统一的校验与赋值入口（构造器与修改共用）
   *
   * @param operationId 工序Id
   * @param workCenterId 工作中心Id
   * @param setupTime 准备时间（分钟，每批一次）
   * @param laborTime 人工时间（分钟/件）
   * @param machineTime 机器时间（分钟/件）
   * @param sequence 工序顺序
   * @throws DomainException 工序、工作中心、顺序非法，或任一段工时为负数时抛出
   */
  private void apply(
      long operationId,
      long workCenterId,
      BigDecimal setupTime,
      BigDecimal laborTime,
      BigDecimal machineTime,
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
    validateNonNegative("准备时间", setupTime);
    validateNonNegative("人工时间", laborTime);
    validateNonNegative("机器时间", machineTime);

    this.operationId = operationId;
    this.workCenterId = workCenterId;
    this.setupTime = setupTime;
    this.laborTime = laborTime;
    this.machineTime = machineTime;
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
