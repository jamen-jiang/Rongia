package com.rongia.stock.domain.stockentrytype;

import com.rongia.shared.domain.aggregate.UpdateAggregateRoot;
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
 * 库存单据类型（开库存单时下拉里挑的那个「菜单」）
 *
 * <p>一行一个单据种类——「领料出库」「采购入库」「报废」。名称是给人看的，随便改；
 * 编码给程序用：自动开单时按编码反查类型行；方向决定库位该怎么填。
 *
 * <p>方向是程序校验的唯一依据：入库只填入库库位、出库只填出库库位、出入两头都填。
 * 要加新种类（报废、借出），建一行、选个方向就行，代码不用动。
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
      @Index(name = "IX_StockEntryType_Direction", columnList = "Direction"),
      @Index(name = "IX_StockEntryType_Sequence", columnList = "Sequence")
      // 注意：Code、Name 的过滤唯一索引带 WHERE IsDeleted=0，JPA 不支持，用 Flyway 创建
    })
public class StockEntryType extends UpdateAggregateRoot {

  /** 编码（程序按它反查类型行，如 ProductionIssue） */
  @Nationalized
  @Column(name = "Code", nullable = false, length = 50)
  private String code;

  /** 名称（人读，随便改） */
  @Nationalized
  @Column(name = "Name", nullable = false, length = 200)
  private String name;

  /** 方向编码，取值见 {@link StockEntryDirectionEnum}：入库 / 出库 / 出入 */
  @Column(name = "Direction", nullable = false, length = 50)
  private String direction;

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
   * @param code 编码
   * @param name 名称
   * @param direction 方向编码
   * @param disabled 是否停用
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   */
  StockEntryType(
      String code,
      String name,
      String direction,
      boolean disabled,
      @Nullable Integer sequence,
      @Nullable String remark) {
    apply(code, name, direction, disabled, sequence, remark);
  }

  /**
   * 修改类型
   *
   * @param code 编码
   * @param name 名称
   * @param direction 方向编码
   * @param disabled 是否停用
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   */
  void modify(
      String code,
      String name,
      String direction,
      boolean disabled,
      @Nullable Integer sequence,
      @Nullable String remark) {
    apply(code, name, direction, disabled, sequence, remark);
  }

  // ==================== 私有：校验与赋值 ====================

  /**
   * 统一的校验与赋值入口（构造器与修改共用）
   *
   * @param code 编码
   * @param name 名称
   * @param direction 方向编码
   * @param disabled 是否停用
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   * @throws DomainException 名称或编码为空、方向非法时抛出
   */
  private void apply(
      String code,
      String name,
      String direction,
      boolean disabled,
      @Nullable Integer sequence,
      @Nullable String remark) {
    String trimmedName = StringUtils.trimToNull(name);
    if (trimmedName == null) {
      throw new DomainException("类型名称不能为空");
    }
    String trimmedCode = StringUtils.trimToNull(code);
    if (trimmedCode == null) {
      throw new DomainException("类型编码不能为空");
    }
    if (EnumUtils.fromCode(StockEntryTypeEnum.class, trimmedCode) != null) {
      throw new DomainException(
          "编码" + trimmedCode + "是程序保留的标准编码，不能用作自建类型");
    }
    String validDirection = EnumUtils.getCode(StockEntryDirectionEnum.class, direction, "方向");

    this.name = trimmedName;
    this.code = trimmedCode;
    this.direction = validDirection;
    this.disabled = disabled;
    this.sequence = sequence;
    this.remark = remark;
  }
}
