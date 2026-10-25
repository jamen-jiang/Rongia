package com.rongia.production.domain.routing;

import com.rongia.shared.domain.aggregate.UpdateAggregateRoot;
import com.rongia.shared.domain.exception.DomainException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.SQLRestriction;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 工艺路线（挂在物料系列上）
 *
 * <p>路线按 SPU 维护，不按 SKU：同一系列下几百个度数走的是同一套工序，挂到变体等于给每个度数复制一份。
 * 个别度数要走别的路线，在工单上直接指定即可。
 *
 * <p>同一物料可以挂多条路线，靠版本号区分；{@code disabled} 只表示停用（不再进工单选择），与版本无关。
 * 工单下达时取未停用的那一条，并把工序快照进工单，之后路线怎么改都不影响在制工单。
 *
 * <p>没有状态，也没有生效区间：路线改了就是改了，历史靠工单上的快照回溯，不靠版本链接力。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "Routing",
    indexes = {
      @Index(name = "IX_Routing_Code", columnList = "Code"),
      @Index(name = "IX_Routing_Name", columnList = "Name"),
      @Index(name = "IX_Routing_Disabled", columnList = "Disabled")
      // 注意：Code + Version 的过滤唯一索引带 WHERE IsDeleted=0，JPA 不支持，用 Flyway 创建
    })
public class Routing extends UpdateAggregateRoot {

  /** 路线代码 */
  @Nationalized
  @Column(name = "Code", nullable = false, length = 100)
  private String code;

  /** 路线名称 */
  @Nationalized
  @Column(name = "Name", nullable = false, length = 200)
  private String name;

  /** 版本号：与物料系列共同构成唯一键，升版就是换一个号 */
  @Nationalized
  @Column(name = "Version", nullable = false, length = 50)
  private String version;

  /** 是否禁用：禁用后下达工单不再选它，已按它下达的工单不受影响 */
  @Column(name = "Disabled", nullable = false)
  private Boolean disabled = false;

  /** 备注（可空） */
  @Nationalized
  @Column(name = "Remark", length = 500)
  @Nullable
  private String remark;

  /** 路线工序列表 */
  @OneToMany(
      fetch = FetchType.EAGER,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE},
      orphanRemoval = true)
  @JoinColumn(name = "RoutingId")
  @SQLRestriction("IsDeleted = 0")
  private List<RoutingOperation> routingOperations = new ArrayList<>();

  /**
   * 获取路线工序列表（只读）
   *
   * @return 不可变列表
   */
  public List<RoutingOperation> getRoutingOperations() {
    return Collections.unmodifiableList(routingOperations);
  }

  /**
   * 构造器
   *
   * @param code 路线代码
   * @param name 路线名称
   * @param version 版本号
   * @param disabled 是否禁用
   * @param remark 备注（可空）
   */
  Routing(
      String code,
      String name,
      String version,
      boolean disabled,
      @Nullable String remark) {
    apply(code, name, version, disabled, remark);
  }

  /**
   * 修改工艺路线
   *
   * @param code 路线代码
   * @param name 路线名称
   * @param version 版本号
   * @param disabled 是否禁用
   * @param remark 备注（可空）
   */
  void modify(
      String code,
      String name,
      String version,
      boolean disabled,
      @Nullable String remark) {
    apply(code, name, version, disabled, remark);
  }

  /**
   * 添加工序
   *
   * @param operationId 工序Id
   * @param workCenterId 工作中心Id
   * @param cycleTimeMode 周期时间计算方式编码
   * @param cycleTime 周期时间（分钟）
   * @param sequence 工序顺序
   * @throws DomainException 工序顺序重复时抛出
   */
  public void addOperation(
      long operationId,
      long workCenterId,
      String cycleTimeMode,
      BigDecimal cycleTime,
      int sequence) {
    if (isExistSequence(sequence, null)) {
      throw new DomainException("工序顺序" + sequence + "已存在!");
    }
    routingOperations.add(
        new RoutingOperation(operationId, workCenterId, cycleTimeMode, cycleTime, sequence));
  }

  /**
   * 修改工序
   *
   * @param routingOperationId 路线工序Id
   * @param operationId 工序Id
   * @param workCenterId 工作中心Id
   * @param cycleTimeMode 周期时间计算方式编码
   * @param cycleTime 周期时间（分钟）
   * @param sequence 工序顺序
   * @throws DomainException 路线工序不存在、工序顺序重复时抛出
   */
  public void modifyOperation(
      long routingOperationId,
      long operationId,
      long workCenterId,
      String cycleTimeMode,
      BigDecimal cycleTime,
      int sequence) {
    RoutingOperation target = findOperation(routingOperationId);
    if (target == null) {
      throw new DomainException("路线工序不存在!");
    }
    if (isExistSequence(sequence, routingOperationId)) {
      throw new DomainException("工序顺序" + sequence + "已存在!");
    }
    target.modify(operationId, workCenterId, cycleTimeMode, cycleTime, sequence);
  }

  /**
   * 删除工序（逻辑删除）
   *
   * @param routingOperationId 路线工序Id
   */
  public void deleteOperation(long routingOperationId) {
    RoutingOperation target = findOperation(routingOperationId);
    if (target == null) {
      return;
    }
    target.delete();
  }

  // ==================== 私有：校验与赋值 ====================

  /**
   * 统一的校验与赋值入口（构造器与修改共用）
   *
   * @param code 路线代码
   * @param name 路线名称
   * @param version 版本号
   * @param disabled 是否禁用
   * @param remark 备注（可空）
   * @throws DomainException 物料系列、代码、名称或版本为空时抛出
   */
  private void apply(
      String code,
      String name,
      String version,
      boolean disabled,
      @Nullable String remark) {
    String trimmedCode = StringUtils.trimToNull(code);
    String trimmedName = StringUtils.trimToNull(name);
    String trimmedVersion = StringUtils.trimToNull(version);
    if (trimmedCode == null) {
      throw new DomainException("工艺路线代码不能为空");
    }
    if (trimmedName == null) {
      throw new DomainException("工艺路线名称不能为空");
    }
    if (trimmedVersion == null) {
      throw new DomainException("版本号不能为空");
    }

    this.code = trimmedCode;
    this.name = trimmedName;
    this.version = trimmedVersion;
    this.disabled = disabled;
    this.remark = remark;
  }

  /**
   * 在路线工序列表中按 Id 查找未删除的工序
   *
   * @param routingOperationId 路线工序Id
   * @return 匹配到的工序，找不到返回 {@code null}
   */
  @Nullable
  private RoutingOperation findOperation(long routingOperationId) {
    return routingOperations.stream()
        .filter(o -> Objects.equals(o.getId(), routingOperationId) && !o.getIsDeleted())
        .findFirst()
        .orElse(null);
  }

  /**
   * 判断工序顺序是否已被占用
   *
   * <p>工序顺序在单条路线内必须唯一；修改工序时需排除自身，否则会把「保持原顺序」误判为冲突。
   *
   * @param sequence 待校验的工序顺序
   * @param excludeId 需要排除的路线工序Id（新增时传 {@code null}）
   * @return true：已被其它未删除的工序占用
   */
  private boolean isExistSequence(int sequence, @Nullable Long excludeId) {
    return routingOperations.stream()
        .filter(o -> !o.getIsDeleted())
        .filter(o -> !Objects.equals(o.getId(), excludeId))
        .anyMatch(o -> Objects.equals(o.getSequence(), sequence));
  }
}
