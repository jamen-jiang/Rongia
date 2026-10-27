package com.rongia.stock.domain.warehouse;

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
 * 仓库
 *
 * <p>仓库是库存记账的边界：一笔数量落在哪个仓，决定它在报表上算哪一笔。它自己不做树——仓下的货位
 * （货架、暂存区、供应商位、客户位）都由 {@code Location} 表达，库位才是树。
 *
 * <p>「原料仓、车间仓、成品仓」各建一个仓库，不靠类型标签区分；仓库本身只有编码、名称、停用和备注四件事。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "Warehouse",
    indexes = {
      @Index(name = "IX_Warehouse_Code", columnList = "Code"),
      @Index(name = "IX_Warehouse_Name", columnList = "Name")
      // 注意：Code 的过滤唯一索引带 WHERE IsDeleted=0，JPA 不支持，用 Flyway 创建
    })
public class Warehouse extends UpdateAggregateRoot {

  /** 代码 */
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
   * @param code 代码
   * @param name 名称
   * @param sequence 排序
   * @param disabled 是否停用
   * @param remark 备注（可空）
   */
  Warehouse(String code, String name, int sequence, boolean disabled, @Nullable String remark) {
    apply(code, name, sequence, disabled, remark);
  }

  /**
   * 修改仓库
   *
   * @param code 代码
   * @param name 名称
   * @param sequence 排序
   * @param disabled 是否停用
   * @param remark 备注（可空）
   */
  void modify(String code, String name, int sequence, boolean disabled, @Nullable String remark) {
    apply(code, name, sequence, disabled, remark);
  }

  // ==================== 私有：校验与赋值 ====================

  /**
   * 统一的校验与赋值入口（构造器与修改共用）
   *
   * @param code 代码
   * @param name 名称
   * @param sequence 排序
   * @param disabled 是否停用
   * @param remark 备注（可空）
   * @throws DomainException 代码或名称为空、或排序非正时抛出
   */
  private void apply(
      String code, String name, int sequence, boolean disabled, @Nullable String remark) {
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

    this.code = trimmedCode;
    this.name = trimmedName;
    this.sequence = sequence;
    this.disabled = disabled;
    this.remark = StringUtils.trimToNull(remark);
  }
}
