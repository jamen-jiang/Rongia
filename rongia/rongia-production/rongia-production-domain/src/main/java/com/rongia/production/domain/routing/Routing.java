package com.rongia.production.domain.routing;

import com.rongia.shared.domain.aggregate.UpdateAggregateRoot;
import com.rongia.shared.domain.exception.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Nationalized;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 工艺路线（挂在物料系列上）
 *
 * <p>路线按 SPU 维护，不按 SKU：同一系列下几百个度数走的是同一套工序，挂到变体等于给每个度数复制一份。
 * 个别度数要走别的路线，在工单上直接指定即可。
 *
 * <p>工序不在本聚合里：它们是独立聚合 {@code BomOperation}，靠 {@code ParentType=Routing} + {@code ParentId} 挂上来。
 * 这样改一道工序不必把整条路线连同全部工序读进来再整体保存。
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
