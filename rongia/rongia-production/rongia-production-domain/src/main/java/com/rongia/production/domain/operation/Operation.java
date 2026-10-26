package com.rongia.production.domain.operation;

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
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Nationalized;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 工序（工艺字典）
 *
 * <p>描述「这道工序本身是什么」，外加建路线时用得上的默认值：默认工作中心、默认周期时间、默认批量。
 * 默认值只在新建路线行时带出来一次，之后按路线改，路线行上的值才是权威——字典上的数变了不会回灌已有路线。
 *
 * <p>字段刻意保持精简：工序表一旦膨胀，工艺员维护不动，最后一定退化成没人看的垃圾字段。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "Operation",
    indexes = {
      @Index(name = "IX_Operation_Code", columnList = "Code"),
      @Index(name = "IX_Operation_Name", columnList = "Name")
      // 注意：Code 的过滤唯一索引带 WHERE IsDeleted=0，JPA 不支持，用 Flyway 创建
    })
public class Operation extends UpdateAggregateRoot {

  /** 工序代码 */
  @Nationalized
  @Column(name = "Code", nullable = false, length = 100)
  private String code;

  /** 工序名称 */
  @Nationalized
  @Column(name = "Name", nullable = false, length = 200)
  private String name;

  /** 默认工作中心Id（可空，照 ERPNext 的 Operation.workstation）：建路线行时带出来，之后按路线改 */
  @Column(name = "WorkCenterId")
  @Nullable
  private Long workCenterId;

  /** 默认周期时间（分钟，可空，照 ERPNext 的 total_operation_time）：建路线行时带出来，之后按路线改 */
  @Column(name = "CycleTime", precision = 18, scale = 2)
  @Nullable
  private BigDecimal cycleTime;

  /** 默认批量大小（件，可空，照 ERPNext 的 batch_size）：建路线行时带出来，之后按路线改 */
  @Column(name = "BatchSize", precision = 18, scale = 2)
  @Nullable
  private BigDecimal batchSize;

  /** 备注（可空） */
  @Nationalized
  @Column(name = "Remark", length = 500)
  @Nullable
  private String remark;

  /**
   * 构造器
   *
   * @param code 工序代码
   * @param name 工序名称
   * @param workCenterId 默认工作中心Id（可空）
   * @param cycleTime 默认周期时间（分钟，可空）
   * @param batchSize 默认批量大小（件，可空）
   * @param remark 备注（可空）
   */
  Operation(
      String code,
      String name,
      @Nullable Long workCenterId,
      @Nullable BigDecimal cycleTime,
      @Nullable BigDecimal batchSize,
      @Nullable String remark) {
    apply(code, name, workCenterId, cycleTime, batchSize, remark);
  }

  /**
   * 修改工序
   *
   * @param code 工序代码
   * @param name 工序名称
   * @param workCenterId 默认工作中心Id（可空）
   * @param cycleTime 默认周期时间（分钟，可空）
   * @param batchSize 默认批量大小（件，可空）
   * @param remark 备注（可空）
   */
  void modify(
      String code,
      String name,
      @Nullable Long workCenterId,
      @Nullable BigDecimal cycleTime,
      @Nullable BigDecimal batchSize,
      @Nullable String remark) {
    apply(code, name, workCenterId, cycleTime, batchSize, remark);
  }

  // ==================== 私有：校验与赋值 ====================

  /**
   * 统一的校验与赋值入口（构造器与修改共用）
   *
   * <p>把「去空格 → 非空校验 → 数值校验 → 赋值」收敛到一处，避免构造器和 {@link #modify} 各写一遍导致规则漂移。
   *
   * @param code 工序代码
   * @param name 工序名称
   * @param workCenterId 默认工作中心Id（可空）
   * @param cycleTime 默认周期时间（分钟，可空）
   * @param batchSize 默认批量大小（件，可空）
   * @param remark 备注（可空）
   * @throws DomainException 代码或名称为空、默认值为负数或工作中心非正时抛出
   */
  private void apply(
      String code,
      String name,
      @Nullable Long workCenterId,
      @Nullable BigDecimal cycleTime,
      @Nullable BigDecimal batchSize,
      @Nullable String remark) {
    String trimmedCode = StringUtils.trimToNull(code);
    String trimmedName = StringUtils.trimToNull(name);
    if (trimmedCode == null) {
      throw new DomainException("工序代码不能为空");
    }
    if (trimmedName == null) {
      throw new DomainException("工序名称不能为空");
    }
    if (workCenterId != null && workCenterId <= 0) {
      throw new DomainException("工作中心不能为空");
    }
    if (cycleTime != null && cycleTime.signum() < 0) {
      throw new DomainException("周期时间不能为负数");
    }
    if (batchSize != null && batchSize.signum() < 0) {
      throw new DomainException("批量大小不能为负数");
    }

    this.code = trimmedCode;
    this.name = trimmedName;
    this.workCenterId = workCenterId;
    this.cycleTime = cycleTime;
    this.batchSize = batchSize;
    this.remark = remark;
  }
}
