package com.rongia.basedata.domain.bank;

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
 * 银行
 *
 * <p>银行是独立主数据，账号行只引用它，不再把行名写成自由文本：Odoo 的 {@code res.bank}（name + 地址 + BIC）、
 * ERPNext 的 {@code Bank}（bank_name + swift_number）都是这么分的，SAP 的 {@code LFBK} 也是
 * 银行代码 {@code BANKL} + 账号 {@code BANKN} 两列。写成文本的话，同一家银行会被录成「建行 / 建设银行 /
 * 中国建设银行XX支行」，付款单打印和按银行汇总时对不上。
 *
 * <p>这里只存编码和名称：SWIFT/BIC、地址、联系电话属于跨境汇款和银行档案的完整信息，
 * 等真有外币付款或银企直连时再加，是纯增量、不改变形状。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "Bank",
    indexes = {
      @Index(name = "IX_Bank_Code", columnList = "Code"),
      @Index(name = "IX_Bank_Name", columnList = "Name"),
      @Index(name = "IX_Bank_Sequence", columnList = "Sequence")
      // 注意：Code 的过滤唯一索引带 WHERE IsDeleted=0，JPA 不支持，用 Flyway 创建
    })
public class Bank extends UpdateAggregateRoot {

  /** 代码 */
  @Nationalized
  @Column(name = "Code", nullable = false, length = 100)
  private String code;

  /** 名称 */
  @Nationalized
  @Column(name = "Name", nullable = false, length = 200)
  private String name;

  /** 排序（可空） */
  @Column(name = "Sequence")
  @Nullable
  private Integer sequence;

  /** 是否禁用 */
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
   * @param disabled 是否禁用
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   */
  Bank(
      String code, String name, boolean disabled, @Nullable Integer sequence, @Nullable String remark) {
    apply(code, name, disabled, sequence, remark);
  }

  /**
   * 修改银行
   *
   * @param code 代码
   * @param name 名称
   * @param disabled 是否禁用
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   */
  void modify(
      String code, String name, boolean disabled, @Nullable Integer sequence, @Nullable String remark) {
    apply(code, name, disabled, sequence, remark);
  }

  // ==================== 私有：校验与赋值 ====================

  /**
   * 统一的校验与赋值入口（构造器与修改共用）
   *
   * @param code 代码
   * @param name 名称
   * @param disabled 是否禁用
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   * @throws DomainException 代码或名称为空时抛出
   */
  private void apply(
      String code, String name, boolean disabled, @Nullable Integer sequence, @Nullable String remark) {
    String trimmedCode = StringUtils.trimToNull(code);
    String trimmedName = StringUtils.trimToNull(name);
    if (trimmedCode == null) {
      throw new DomainException("银行代码不能为空");
    }
    if (trimmedName == null) {
      throw new DomainException("银行名称不能为空");
    }

    this.code = trimmedCode;
    this.name = trimmedName;
    this.disabled = disabled;
    this.sequence = sequence;
    this.remark = remark;
  }
}
