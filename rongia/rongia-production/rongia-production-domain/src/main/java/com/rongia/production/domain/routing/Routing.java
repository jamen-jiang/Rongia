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
 * <p>工序是路线的子行 {@link RoutingOperation}：改一道工序要连同路线一起保存，但一份路线的工序也就几道，
 * 整体读写的代价远小于两栖表带来的父子混乱。
 *
 * <p>没有版本，也没有生效区间：路线改了就是改了，历史靠工单上的快照回溯。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "Routing",
    indexes = {
      @Index(name = "IX_Routing_Name", columnList = "Name"),
      @Index(name = "IX_Routing_Disabled", columnList = "Disabled")
    })
public class Routing extends UpdateAggregateRoot {

  /** 路线名称 */
  @Nationalized
  @Column(name = "Name", nullable = false, length = 200)
  private String name;

  /** 是否禁用：禁用后下达工单不再选它，已按它下达的工单不受影响 */
  @Column(name = "Disabled", nullable = false)
  private Boolean disabled = false;

  /** 备注（可空） */
  @Nationalized
  @Column(name = "Remark", length = 500)
  @Nullable
  private String remark;

  /** 路线工序（按顺序） */
  @OneToMany(
      fetch = FetchType.EAGER,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE},
      orphanRemoval = true)
  @JoinColumn(name = "RoutingId")
  @SQLRestriction("IsDeleted = 0")
  private List<RoutingOperation> routingOperations = new ArrayList<>();

  /**
   * 构造器
   *
   * @param name 路线名称
   * @param disabled 是否禁用
   * @param remark 备注（可空）
   */
  Routing(String name, boolean disabled, @Nullable String remark) {
    apply(name, disabled, remark);
  }

  /**
   * 修改工艺路线
   *
   * @param name 路线名称
   * @param disabled 是否禁用
   * @param remark 备注（可空）
   */
  void modify(String name, boolean disabled, @Nullable String remark) {
    apply(name, disabled, remark);
  }

  /**
   * 获取路线工序（只读）
   *
   * @return 不可变列表
   */
  public List<RoutingOperation> getRoutingOperations() {
    return Collections.unmodifiableList(routingOperations);
  }

  /**
   * 添加工序行
   *
   * @param operationId 工序Id
   * @param workCenterId 工作中心Id
   * @param setupTime 准备时间（分钟，每批一次）
   * @param laborTime 人工时间（分钟/件）
   * @param machineTime 机器时间（分钟/件）
   * @param sequence 工序顺序
   * @throws DomainException 工序顺序重复时抛出
   */
  public void addOperation(
      long operationId,
      long workCenterId,
      BigDecimal setupTime,
      BigDecimal laborTime,
      BigDecimal machineTime,
      int sequence) {
    if (findOperationBySequence(sequence) != null) {
      throw new DomainException("工序顺序" + sequence + "重复!");
    }
    routingOperations.add(
        new RoutingOperation(
            operationId, workCenterId, setupTime, laborTime, machineTime, sequence));
  }

  /**
   * 移除工序行
   *
   * @param routingOperationId 路线工序Id
   */
  public void removeOperation(long routingOperationId) {
    routingOperations.stream()
        .filter(o -> Objects.equals(o.getId(), routingOperationId))
        .findFirst()
        .ifPresent(routingOperations::remove);
  }

  /**
   * 按顺序查找未删除的工序行
   *
   * @param sequence 工序顺序
   * @return 匹配到的工序行，找不到返回 {@code null}
   */
  @Nullable
  private RoutingOperation findOperationBySequence(int sequence) {
    return routingOperations.stream()
        .filter(o -> !o.getIsDeleted())
        .filter(o -> Objects.equals(o.getSequence(), sequence))
        .findFirst()
        .orElse(null);
  }

  // ==================== 私有：校验与赋值 ====================

  /**
   * 统一的校验与赋值入口（构造器与修改共用）
   *
   * @param name 路线名称
   * @param disabled 是否禁用
   * @param remark 备注（可空）
   * @throws DomainException 名称为空时抛出
   */
  private void apply(String name, boolean disabled, @Nullable String remark) {
    String trimmedName = StringUtils.trimToNull(name);
    if (trimmedName == null) {
      throw new DomainException("工艺路线名称不能为空");
    }

    this.name = trimmedName;
    this.disabled = disabled;
    this.remark = remark;
  }
}
