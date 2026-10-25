package com.rongia.inventory.domain.stockentrytype;

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
 * 库存单据类型（开库存单时下拉里挑的那个「菜单」）
 *
 * <p>一行一个单据种类——「领料」「完工入库」「调拨」。单子上没有「入库」「出库」这一说，货往哪走由行上的
 * 源仓/目标仓决定；类型只负责给单子贴归堆标签（报表按它分类、单头显示它），跟 ERPNext 的
 * {@code Stock Entry Type} 一个道理。
 *
 * <p>每个类型有一个机读编码：程序生成单据时按行上的仓推出固定编码（领料出库、收货入库、调拨三个字符串常量），
 * 再按编码反查标准类型行落库——ERPNext 的 {@code make_stock_entry} 就是这么干的（仓怎么填推 purpose，
 * purpose 反查 is_standard=1 的类型行）。编码对标准行是程序认的唯一名，永不改；手工开单的自建类型编码随意起，
 * 程序不校验不拦截。名称是给人看的，随便改。
 *
 * <p>系统预置哪几行由初始化数据决定，这里不管：类型是纯配置，加一行是运行时的事，表结构和代码都不用动——
 * 这正是把它做成表而不是枚举的理由。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "StockEntryType",
    indexes = {
      @Index(name = "IX_StockEntryType_Code", columnList = "Code"),
      @Index(name = "IX_StockEntryType_Name", columnList = "Name"),
      @Index(name = "IX_StockEntryType_Sequence", columnList = "Sequence")
      // 注意：Code、Name 的过滤唯一索引带 WHERE IsDeleted=0，JPA 不支持，用 Flyway 创建
    })
public class StockEntryType extends UpdateAggregateRoot {

  /** 类型编码（机读，取值见 {@link StockEntryTypeEnum}，程序按它反查类型行） */
  @Nationalized
  @Column(name = "Code", nullable = false, length = 50)
  private String code;

  /** 名称 */
  @Nationalized
  @Column(name = "Name", nullable = false, length = 200)
  private String name;

  /** 是否标准类型（程序反查只认标准行；非标准行是手工开单的菜单项） */
  @Column(name = "IsStandard", nullable = false)
  private Boolean isStandard;

  /** 排序（可空） */
  @Column(name = "Sequence")
  @Nullable
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
   * @param code 类型编码
   * @param name 名称
   * @param isStandard 是否标准类型
   * @param disabled 是否停用
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   */
  StockEntryType(
      String code,
      String name,
      boolean isStandard,
      boolean disabled,
      @Nullable Integer sequence,
      @Nullable String remark) {
    apply(code, name, isStandard, disabled, sequence, remark);
  }

  /**
   * 修改类型
   *
   * @param code 类型编码
   * @param name 名称
   * @param isStandard 是否标准类型
   * @param disabled 是否停用
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   */
  void modify(
      String code,
      String name,
      boolean isStandard,
      boolean disabled,
      @Nullable Integer sequence,
      @Nullable String remark) {
    apply(code, name, isStandard, disabled, sequence, remark);
  }

  // ==================== 私有：校验与赋值 ====================

  /**
   * 统一的校验与赋值入口（构造器与修改共用）
   *
   * <p>编码不做枚举校验：程序自动生成单据只认三个固定编码（领料出库、收货入库、调拨，见
   * {@code StockEntryDomainService} 里的常量），其他手工开单用的类型编码随意起，程序不拦。
   *
   * @param code 类型编码
   * @param name 名称
   * @param isStandard 是否标准类型
   * @param disabled 是否停用
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   * @throws DomainException 编码或名称为空时抛出
   */
  private void apply(
      String code,
      String name,
      boolean isStandard,
      boolean disabled,
      @Nullable Integer sequence,
      @Nullable String remark) {
    String trimmedCode = StringUtils.trimToNull(code);
    if (trimmedCode == null) {
      throw new DomainException("类型编码不能为空");
    }
    String trimmedName = StringUtils.trimToNull(name);
    if (trimmedName == null) {
      throw new DomainException("类型名称不能为空");
    }

    this.code = trimmedCode;
    this.name = trimmedName;
    this.isStandard = isStandard;
    this.disabled = disabled;
    this.sequence = sequence;
    this.remark = remark;
  }
}
