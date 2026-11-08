package com.rongia.stock.domain.warehouse;

import com.rongia.shared.domain.aggregate.UpdateAggregateRoot;
import com.rongia.shared.domain.entity.Parent;
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
 * 仓库（存货点树，照 ERPNext 的 {@code stock.warehouse} 单表形状）
 *
 * <p>仓库本身就是树：叶子（{@code isLeaf = true}）是存货点——货实际待的地方，库存行（{@code Stock}）
 * 只挂在叶子上；非叶子是分组节点——「原料仓」「A 区」这类组织伞，只用来归堆和汇总，不存货
 * （ERPNext 的 {@code is_group}，这里反过来用 {@code isLeaf} 表达，存货点是默认值）。
 *
 * <p>树照 Odoo 的 {@code location_id + parent_path} 形状：「原料仓 → A 区 → 01 架」这类层级直接挂在
 * 树里，不靠名字拼接。以前「仓库」与「库位」是两张表，库位必须挂仓——但库存本来就记在节点上、
 * 不记在仓上，两张表只是把同一棵树切成了两段；合成一张后，「仓库」既是边界也是位置，
 * 虚拟场所（在途、盘盈亏）就是树上一个普通叶子。
 *
 * <p>它只回答「货放在哪个位置」，不回答「这笔出入是什么业务」——方向与用途由单据类型表达
 * （{@code StockEntryType.Direction}），同一个存货点能被入库单和出库单反复使用。
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
      @Index(name = "IX_Warehouse_Name", columnList = "Name")
      // 注意：ParentId + Code 的过滤唯一索引带 WHERE IsDeleted=0，JPA 不支持，用 Flyway 创建
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

  /** 是否叶子（叶子=存货点，库存只挂叶子；非叶子=分组节点，只归堆不存货） */
  @Column(name = "IsLeaf", nullable = false)
  private Boolean isLeaf;

  /** 代码（同级下唯一，如 "A-01"） */
  @Nationalized
  @Column(name = "Code", nullable = false, length = 100)
  private String code;

  /** 名称 */
  @Nationalized
  @Column(name = "Name", nullable = false, length = 200)
  private String name;

  /** 排序（列表展示顺序，照 Odoo 的 sequence） */
  @Column(name = "Sequence", nullable = false)
  private Integer sequence;

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
   * @param isLeaf 是否叶子（叶子=存货点）
   * @param code 代码
   * @param name 名称
   * @param sequence 排序
   * @param disabled 是否停用
   * @param remark 备注（可空）
   */
  Warehouse(
      @Nullable Long parentId,
      @Nullable String parentPath,
      boolean isLeaf,
      String code,
      String name,
      int sequence,
      boolean disabled,
      @Nullable String remark) {
    apply(parentId, parentPath, isLeaf, code, name, sequence, disabled, remark);
  }

  /**
   * 修改仓库
   *
   * @param parentId 上级仓库Id（可空）
   * @param parentPath 层级路径（可空）
   * @param isLeaf 是否叶子（叶子=存货点）
   * @param code 代码
   * @param name 名称
   * @param sequence 排序
   * @param disabled 是否停用
   * @param remark 备注（可空）
   */
  void modify(
      @Nullable Long parentId,
      @Nullable String parentPath,
      boolean isLeaf,
      String code,
      String name,
      int sequence,
      boolean disabled,
      @Nullable String remark) {
    apply(parentId, parentPath, isLeaf, code, name, sequence, disabled, remark);
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
   * @param isLeaf 是否叶子（叶子=存货点）
   * @param code 代码
   * @param name 名称
   * @param sequence 排序
   * @param disabled 是否停用
   * @param remark 备注（可空）
   * @throws DomainException 代码或名称为空、排序非正、或上级仓库Id有误时抛出
   */
  private void apply(
      @Nullable Long parentId,
      @Nullable String parentPath,
      boolean isLeaf,
      String code,
      String name,
      int sequence,
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
    if (sequence <= 0) {
      throw new DomainException("排序必须大于0");
    }
    if (parentId != null && parentId <= 0) {
      throw new DomainException("上级仓库" + parentId + "有误!");
    }

    this.parentId = parentId;
    this.parentPath = parentPath;
    this.isLeaf = isLeaf;
    this.code = trimmedCode;
    this.name = trimmedName;
    this.sequence = sequence;
    this.disabled = disabled;
    this.remark = StringUtils.trimToNull(remark);
  }
}
