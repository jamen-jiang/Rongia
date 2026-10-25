package com.rongia.inventory.domain.warehouse;

import com.rongia.shared.domain.aggregate.UpdateAggregateRoot;
import com.rongia.shared.domain.entity.Parent;
import com.rongia.shared.domain.exception.DomainException;
import com.rongia.shared.domain.util.HierarchyUtils;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
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
 * 仓库（树）
 *
 * <p>仓库是库存记账的边界：一笔数量落在哪个仓，决定它在报表上算哪一笔。仓库本身不带类型标签——需要「原料仓、
 * 车间仓、成品仓」这类区分时，就各建一个仓库，由引用它的那张单据来表达用途（领料单行上的发料仓、
 * 完工入库单上的收货仓都是这么用的；工单头上的 {@code WarehouseId} 只是入库单没填仓库时的默认收货仓）。
 * 这是 D365 的口径：仓库是库存维度之一，「usually a geographic location or a particular function」，功能靠建仓
 * 而不是靠打标签。
 *
 * <p>仓库是树（照 ERPNext 的 Warehouse：parent_warehouse + is_group）：「成品仓 → 成品仓-A 货架」这类层级
 * 直接挂在树里，不再用单独的库位表——树节点本身就是仓，库存可以记在任何一层。末级标志决定库存落在哪：
 * {@code isLeaf} 为 true 的仓才能持有库存，分组节点只用于往下套子仓，避免「父仓自有数量与子仓数量混算」。
 *
 * <p>「这批数量能不能动用」不在仓库上表达：拿「待检仓」「废品仓」去顶这一维，仓的数量就会随状态翻倍，
 * 等有单据承载处置时再定形态。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "Warehouse",
    indexes = {
      @Index(name = "IX_Warehouse_ParentId", columnList = "ParentId"),
      @Index(name = "IX_Warehouse_ParentPath", columnList = "ParentPath"),
      @Index(name = "IX_Warehouse_Code", columnList = "Code"),
      @Index(name = "IX_Warehouse_Name", columnList = "Name"),
      @Index(name = "IX_Warehouse_IsLeaf", columnList = "IsLeaf")
      // 注意：Code 的过滤唯一索引带 WHERE IsDeleted=0，JPA 不支持，用 Flyway 创建
    })
public class Warehouse extends UpdateAggregateRoot implements Parent {

  /** 上级仓库Id（可空，根节点为空） */
  @Column(name = "ParentId")
  @Nullable
  private Long parentId;

  /** 层级路径（可空，只存祖先、不含自己，形如 "1-3-5"，由层级编排逻辑维护） */
  @Nationalized
  @Column(name = "ParentPath")
  @Nullable
  private String parentPath;

  /** 代码 */
  @Nationalized
  @Column(name = "Code", nullable = false, length = 100)
  private String code;

  /** 名称 */
  @Nationalized
  @Column(name = "Name", nullable = false, length = 200)
  private String name;

  /** 是否末级：末级仓才能持有库存，分组节点只用于往下套子仓 */
  @Column(name = "IsLeaf", nullable = false)
  private Boolean isLeaf = true;

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
   * @param parentId 上级仓库Id（可空）
   * @param parentPath 层级路径（可空）
   * @param code 代码
   * @param name 名称
   * @param isLeaf 是否末级
   * @param disabled 是否停用
   * @param remark 备注（可空）
   */
  Warehouse(
      @Nullable Long parentId,
      @Nullable String parentPath,
      String code,
      String name,
      boolean isLeaf,
            boolean disabled,
      @Nullable String remark) {
    apply(parentId, parentPath, code, name, isLeaf, disabled, remark);
  }

  /**
   * 修改仓库
   *
   * @param parentId 上级仓库Id（可空）
   * @param parentPath 层级路径（可空）
   * @param code 代码
   * @param name 名称
   * @param isLeaf 是否末级
   * @param disabled 是否停用
   * @param remark 备注（可空）
   */
  void modify(
      @Nullable Long parentId,
      @Nullable String parentPath,
      String code,
      String name,
      boolean isLeaf,
            boolean disabled,
      @Nullable String remark) {
    apply(parentId, parentPath, code, name, isLeaf, disabled, remark);
  }

  /** 重设层级路径（由领域服务按父节点算好后回填） */
  @Override
  public void changeParentPath(@Nullable String parentPath) {
    this.parentPath = parentPath;
  }

  // ==================== 私有：校验与赋值 ====================

  /**
   * 统一的校验与赋值入口（构造器与修改共用）
   *
   * @param parentId 上级仓库Id（可空）
   * @param parentPath 层级路径（可空）
   * @param code 代码
   * @param name 名称
   * @param isLeaf 是否末级
   * @param disabled 是否停用
   * @param remark 备注（可空）
   * @throws DomainException 代码或名称为空时抛出
   */
  private void apply(
      @Nullable Long parentId,
      @Nullable String parentPath,
      String code,
      String name,
      boolean isLeaf,
            boolean disabled,
      @Nullable String remark) {
    String trimmedCode = StringUtils.trimToNull(code);
    String trimmedName = StringUtils.trimToNull(name);
    if (trimmedCode == null) {
      throw new DomainException("仓库代码不能为空");
    }
    if (trimmedName == null) {
      throw new DomainException("仓库名称不能为空");
    }
    if (parentId != null && parentId <= 0) {
      throw new DomainException("上级仓库" + parentId + "有误!");
    }

    this.parentId = parentId;
    this.parentPath = parentPath;
    this.code = trimmedCode;
    this.name = trimmedName;
    this.isLeaf = isLeaf;
    this.disabled = disabled;
    this.remark = StringUtils.trimToNull(remark);
  }
}
