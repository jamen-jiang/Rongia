package com.rongia.stock.domain.location;

import com.rongia.shared.domain.aggregate.UpdateAggregateRoot;
import com.rongia.shared.domain.entity.Parent;
import com.rongia.shared.domain.exception.DomainException;
import com.rongia.shared.domain.util.EnumUtils;
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
 * 库位（照 Odoo 的 {@code stock.location}）
 *
 * <p>库存记在库位上，不记在仓库上：仓库是分组和单据的边界，库位才是货实际待的地方。
 * 库位本身也是树（照 Odoo 的 {@code location_id + parent_path}）：「A 区 → 01 架」这类层级直接挂在树里，
 * 不再靠名字拼接。
 *
 * <p>{@code Usage} 决定这个库位在出入库里的角色：内部库位真实存货；供应商、客户、生产、库存损失、在途
 * 是虚拟位置，用来表示「货还没进来」「货已经出去了」。结构节点（{@code View}）不能直接存货，只用于往下分组。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "Location",
    indexes = {
      @Index(name = "IX_Location_WarehouseId", columnList = "WarehouseId"),
      @Index(name = "IX_Location_ParentId", columnList = "ParentId"),
      @Index(name = "IX_Location_ParentPath", columnList = "ParentPath"),
      @Index(name = "IX_Location_Code", columnList = "Code"),
      @Index(name = "IX_Location_Usage", columnList = "Usage")
      // 注意：WarehouseId + Code 的过滤唯一索引带 WHERE IsDeleted=0，JPA 不支持，用 Flyway 创建
    })
public class Location extends UpdateAggregateRoot implements Parent {

  /** 所属仓库Id */
  @Column(name = "WarehouseId", nullable = false)
  private Long warehouseId;

  /** 上级库位Id（可空，根节点为空） */
  @Column(name = "ParentId")
  @Nullable
  private Long parentId;

  /** 层级路径（可空，只存祖先、不含自己，形如 "1-3-5"，由层级编排逻辑维护） */
  @Nationalized
  @Column(name = "ParentPath")
  @Nullable
  private String parentPath;

  /** 库位代码（同一仓库内唯一，如 "A-01"） */
  @Nationalized
  @Column(name = "Code", nullable = false, length = 100)
  private String code;

  /** 库位名称 */
  @Nationalized
  @Column(name = "Name", nullable = false, length = 200)
  private String name;

  /** 用途编码，取值见 {@link LocationUsageEnum} */
  @Column(name = "Usage", nullable = false, length = 50)
  private String usage;

  /** 是否停用 */
  @Column(name = "Disabled", nullable = false)
  private Boolean disabled;

  /** 备注（可空） */
  @Nationalized
  @Column(name = "Remark", length = 500)
  @Nullable
  private String remark;

  /**
   * 构造器
   *
   * @param warehouseId 所属仓库Id
   * @param parentId 上级库位Id（可空）
   * @param parentPath 层级路径（可空）
   * @param code 库位代码
   * @param name 库位名称
   * @param usage 用途编码
   * @param disabled 是否停用
   * @param remark 备注（可空）
   */
  Location(
      long warehouseId,
      @Nullable Long parentId,
      @Nullable String parentPath,
      String code,
      String name,
      String usage,
      boolean disabled,
      @Nullable String remark) {
    apply(warehouseId, parentId, parentPath, code, name, usage, disabled, remark);
  }

  /**
   * 修改库位
   *
   * @param warehouseId 所属仓库Id
   * @param parentId 上级库位Id（可空）
   * @param parentPath 层级路径（可空）
   * @param code 库位代码
   * @param name 库位名称
   * @param usage 用途编码
   * @param disabled 是否停用
   * @param remark 备注（可空）
   */
  void modify(
      long warehouseId,
      @Nullable Long parentId,
      @Nullable String parentPath,
      String code,
      String name,
      String usage,
      boolean disabled,
      @Nullable String remark) {
    apply(warehouseId, parentId, parentPath, code, name, usage, disabled, remark);
  }

  /** 重设层级路径（由领域服务按父节点算好后回填） */
  @Override
  public void changeParentPath(@Nullable String parentPath) {
    this.parentPath = parentPath;
  }

  /**
   * 是否内部库位（真实存货）
   *
   * @return true 表示内部库位
   */
  public boolean isInternal() {
    return LocationUsageEnum.INTERNAL.getCode().equals(this.usage);
  }

  /**
   * 是否能直接持有库存
   *
   * @return true 表示除结构节点外都能存货
   */
  public boolean canHoldStock() {
    return !LocationUsageEnum.VIEW.getCode().equals(this.usage);
  }

  // ==================== 私有：校验与赋值 ====================

  /**
   * 统一的校验与赋值入口（构造器与修改共用）
   *
   * @param warehouseId 所属仓库Id
   * @param parentId 上级库位Id（可空）
   * @param parentPath 层级路径（可空）
   * @param code 库位代码
   * @param name 库位名称
   * @param usage 用途编码
   * @param disabled 是否停用
   * @param remark 备注（可空）
   * @throws DomainException 仓库为空、代码或名称为空、用途非法、上级库位Id有误时抛出
   */
  private void apply(
      long warehouseId,
      @Nullable Long parentId,
      @Nullable String parentPath,
      String code,
      String name,
      String usage,
      boolean disabled,
      @Nullable String remark) {
    if (warehouseId <= 0) {
      throw new DomainException("所属仓库不能为空");
    }
    String trimmedCode = StringUtils.trimToNull(code);
    String trimmedName = StringUtils.trimToNull(name);
    if (trimmedCode == null) {
      throw new DomainException("库位代码不能为空");
    }
    if (trimmedName == null) {
      throw new DomainException("库位名称不能为空");
    }
    if (parentId != null && parentId <= 0) {
      throw new DomainException("上级库位" + parentId + "有误!");
    }
    String validUsage = EnumUtils.getCode(LocationUsageEnum.class, usage, "库位用途");

    this.warehouseId = warehouseId;
    this.parentId = parentId;
    this.parentPath = parentPath;
    this.code = trimmedCode;
    this.name = trimmedName;
    this.usage = validUsage;
    this.disabled = disabled;
    this.remark = StringUtils.trimToNull(remark);
  }
}
