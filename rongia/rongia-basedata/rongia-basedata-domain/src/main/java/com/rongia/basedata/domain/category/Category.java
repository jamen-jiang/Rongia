package com.rongia.basedata.domain.category;

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
 * 物料分类（树）
 *
 * <p>用来替代硬编码的物料类型枚举。对过外部系统：SAP 的物料组 {@code MARA-MATKL}、Odoo 的
 * {@code material.category}、ERPNext 的 Item Group、金蝶的物料分组、用友的存货分类，无一例外是
 * 「用户自己维护的树」而不是写死在代码里的枚举——因为产品线一变，枚举就得改代码、发版本。
 *
 * <p>分类只回答「这个物料归到哪一类」，进而决定它挂哪一组规格属性；「这个物料怎么来、要不要管库存」
 * 是另一个维度，见 {@code MaterialTypeEnum} 与物料上那两个"能不能"。SAP 把 {@code MARA-MATKL} 与
 * {@code MARC-BESKZ} 分成两个字段正是这个原因，两者可以任意组合，不要混在一个字段里。
 *
 * <p>末级标志决定物料能落在哪一层：分组节点（非末级）只能往下套分类，物料只能挂在末级分类上。金蝶的「明细」标志、
 * ERPNext Item Group 的 {@code is_group}、用友"非末级不许挂存货"都是这一条。不加这列会出的乱子很具体：
 * 「树脂镜片」自己挂着一批物料、下面又开了「1.56」「1.60」两个子分类，按大类汇总时父类那一堆和子类的库存
 * 混在同一个数里，谁也对不上账。规则是双向互斥的：末级分类下面不许再建子分类，下面有子分类的不许改成末级，
 * 所以这一列不会和实际的树形漂。物料建档与分类删除都要读它——前者由 {@code CategoryDomainService#ensureLeaf}
 * 把关，后者要连带查这一类下有没有物料，归应用层。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "Category",
    indexes = {
      @Index(name = "IX_Category_ParentId", columnList = "ParentId"),
      @Index(name = "IX_Category_ParentPath", columnList = "ParentPath"),
      @Index(name = "IX_Category_Code", columnList = "Code"),
      @Index(name = "IX_Category_Name", columnList = "Name"),
      @Index(name = "IX_Category_Sequence", columnList = "Sequence")
      // 注意：Code 的过滤唯一索引带 WHERE IsDeleted=0，JPA 不支持，用 Flyway 创建
    })
public class Category extends UpdateAggregateRoot implements Parent {

  /** 父分类Id（可空，根节点为空） */
  @Column(name = "ParentId")
  @Nullable
  private Long parentId;

  /** 层级路径（可空，形如 "1-3-5"，由层级编排逻辑维护） */
  @Nationalized
  @Column(name = "ParentPath")
  @Nullable
  private String parentPath;

  /** 分类代码 */
  @Nationalized
  @Column(name = "Code", nullable = false, length = 100)
  private String code;

  /** 分类名称 */
  @Nationalized
  @Column(name = "Name", nullable = false, length = 200)
  private String name;

  /** 是否末级：末级分类才能挂物料，非末级只用于往下套子分类 */
  @Column(name = "IsLeaf", nullable = false)
  private Boolean isLeaf = true;

  /** 排序（可空） */
  @Column(name = "Sequence")
  @Nullable
  private Integer sequence;

  /** 是否禁用 */
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
   * @param parentId 父分类Id（可空）
   * @param parentPath 层级路径（可空）
   * @param code 分类代码
   * @param name 分类名称
   * @param isLeaf 是否末级
   * @param sequence 排序（可空）
   * @param disabled 是否禁用
   * @param remark 备注（可空）
   */
  Category(
      @Nullable Long parentId,
      @Nullable String parentPath,
      String code,
      String name,
      boolean isLeaf,
      @Nullable Integer sequence,
      boolean disabled,
      @Nullable String remark) {
    apply(parentId, parentPath, code, name, isLeaf, sequence, disabled, remark);
  }

  /**
   * 修改分类
   *
   * @param parentId 父分类Id（可空）
   * @param parentPath 层级路径（可空）
   * @param code 分类代码
   * @param name 分类名称
   * @param isLeaf 是否末级
   * @param sequence 排序（可空）
   * @param disabled 是否禁用
   * @param remark 备注（可空）
   */
  void modify(
      @Nullable Long parentId,
      @Nullable String parentPath,
      String code,
      String name,
      boolean isLeaf,
      @Nullable Integer sequence,
      boolean disabled,
      @Nullable String remark) {
    apply(parentId, parentPath, code, name, isLeaf, sequence, disabled, remark);
  }

  /**
   * 重设层级路径
   *
   * @param parentPath 层级路径（可空）
   */
  @Override
  public void changeParentPath(@Nullable String parentPath) {
    this.parentPath = parentPath;
  }

  // ==================== 私有：校验与赋值 ====================

  /**
   * 统一的校验与赋值入口（构造器与修改共用）
   *
   * <p>把「去空格 → 非空校验 → 赋值」收敛到一处，避免构造器和 {@link #modify} 各写一遍导致规则漂移。
   *
   * @param parentId 父分类Id（可空）
   * @param parentPath 层级路径（可空）
   * @param code 分类代码
   * @param name 分类名称
   * @param isLeaf 是否末级
   * @param sequence 排序（可空）
   * @param disabled 是否禁用
   * @param remark 备注（可空）
   */
  private void apply(
      @Nullable Long parentId,
      @Nullable String parentPath,
      String code,
      String name,
      boolean isLeaf,
      @Nullable Integer sequence,
      boolean disabled,
      @Nullable String remark) {
    String trimmedCode = StringUtils.trimToNull(code);
    String trimmedName = StringUtils.trimToNull(name);
    if (trimmedCode == null) {
      throw new DomainException("分类代码不能为空");
    }
    if (trimmedName == null) {
      throw new DomainException("分类名称不能为空");
    }
    this.parentPath = parentPath;
    this.parentId = parentId;
    this.code = trimmedCode;
    this.name = trimmedName;
    this.isLeaf = isLeaf;
    this.disabled = disabled;
    this.sequence = sequence;
    this.remark = remark;
  }
}
