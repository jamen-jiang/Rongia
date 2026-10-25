package com.rongia.basedata.domain.taxrate;

import com.rongia.shared.domain.aggregate.UpdateAggregateRoot;
import com.rongia.shared.domain.exception.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Nationalized;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 税率
 *
 * <p>{@code rate} 按**百分比**存：{@code 13.00} 就是 13%，不是 {@code 0.13}。这个口径必须写死——
 * 同一列一半人填 0.13、一半人填 13，算出来的税差 100 倍，而且从数据上看不出谁对。
 *
 * <p>税率是主数据，单据上的税率是**快照**：以后销售/采购单据的行上同时存税率的 Id 和当时的数值
 * （{@code taxRateId} + {@code rate}），算税用行上的数。只存 Id 的话，主数据改一次税率，历史单据的税额
 * 会跟着变——BC 的 VAT Posting Setup、D365 的税额值都是按发生日取的，没有哪家让历史跟着主数据走。
 *
 * <p>{@code disabled} 是停用一个不再适用的税率（比如政策变了）：停用后新单据不该再选到它，
 * 而历史单据因为存了数值快照，不受影响——这也是为什么税率能停用、却不能随手删。
 *
 * <p>现在不做有效期、税码、税模板：有效期（同一条税率按日期取不同值）和复合税（增值税 + 附加税叠加、
 * 销项/进项分开的税码）都要等真有单据按日期算税时才用得上，提前加只是一张没人读的表。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "TaxRate",
    indexes = {
      @Index(name = "IX_TaxRate_Code", columnList = "Code"),
      @Index(name = "IX_TaxRate_Name", columnList = "Name"),
      @Index(name = "IX_TaxRate_Sequence", columnList = "Sequence")
      // 注意：Code 的过滤唯一索引带 WHERE IsDeleted=0，JPA 不支持，用 Flyway 创建
    })
public class TaxRate extends UpdateAggregateRoot {

  /** 代码 */
  @Nationalized
  @Column(name = "Code", nullable = false, length = 100)
  private String code;

  /** 名称 */
  @Nationalized
  @Column(name = "Name", nullable = false, length = 200)
  private String name;

  /** 税率值（百分比，13.00 表示 13%） */
  @Column(name = "Rate", nullable = false, precision = 18, scale = 2)
  private BigDecimal rate;

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
   * @param code 代码
   * @param name 名称
   * @param rate 税率值（百分比，13.00 = 13%）
   * @param disabled 是否禁用
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   */
  TaxRate(
      String code,
      String name,
      BigDecimal rate,
      boolean disabled,
      @Nullable Integer sequence,
      @Nullable String remark) {
    apply(code, name, rate, disabled, sequence, remark);
  }

  /**
   * 修改税率
   *
   * @param code 代码
   * @param name 名称
   * @param rate 税率值（百分比，13.00 = 13%）
   * @param disabled 是否禁用
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   */
  void modify(
      String code,
      String name,
      BigDecimal rate,
      boolean disabled,
      @Nullable Integer sequence,
      @Nullable String remark) {
    apply(code, name, rate, disabled, sequence, remark);
  }

  // ==================== 私有：校验与赋值 ====================

  /**
   * 统一的校验与赋值入口（构造器与修改共用）
   *
   * <p>把「去空格 → 非空校验 → 赋值」收敛到一处，避免构造器和 {@link #modify} 各写一遍导致规则漂移。
   *
   * @param code 代码
   * @param name 名称
   * @param rate 税率值（百分比，13.00 = 13%）
   * @param disabled 是否禁用
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   */
  private void apply(
      String code,
      String name,
      BigDecimal rate,
      boolean disabled,
      @Nullable Integer sequence,
      @Nullable String remark) {
    String trimmedCode = StringUtils.trimToNull(code);
    String trimmedName = StringUtils.trimToNull(name);
    if (trimmedCode == null) {
      throw new DomainException("税率代码不能为空");
    }
    if (trimmedName == null) {
      throw new DomainException("税率名称不能为空");
    }
    this.code = trimmedCode;
    this.name = trimmedName;
    this.rate = rate;
    this.disabled = disabled;
    this.sequence = sequence;
    this.remark = remark;
  }
}
