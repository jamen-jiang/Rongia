package com.rongia.basedata.domain.currency;

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

/** 币种 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "Currency",
    indexes = {
      @Index(name = "IX_Currency_Code", columnList = "Code"),
      @Index(name = "IX_Currency_Name", columnList = "Name")
      // 注意：Code 的过滤唯一索引带 WHERE IsDeleted=0，JPA 不支持，用 Flyway 创建
    })
public class Currency extends UpdateAggregateRoot {

  /** 币种代码 */
  @Nationalized
  @Column(name = "Code", nullable = false, length = 100)
  private String code;

  /** 币种名称 */
  @Nationalized
  @Column(name = "Name", nullable = false, length = 200)
  private String name;

  /** 币种符号 */
  @Nationalized
  @Column(name = "Symbol", nullable = false, length = 10)
  private String symbol;

  /** 汇率 */
  @Column(name = "AcceptRate", precision = 18, scale = 6)
  private BigDecimal acceptRate;

  /** 调整率 */
  @Column(name = "AdjustRate", precision = 18, scale = 6)
  private BigDecimal adjustRate;

  /** 是否本位币 */
  @Column(name = "IsStandard", nullable = false)
  private Boolean isStandard;

  /** 是否禁用 */
  @Column(name = "Disabled", nullable = false)
  private Boolean disabled;

  /**
   * 构造器
   *
   * @param code 币种代码
   * @param name 币种名称
   * @param symbol 币种符号
   * @param acceptRate 汇率
   * @param adjustRate 调整率
   * @param isStandard 是否本位币
   * @param disabled 是否禁用
   */
  Currency(
      String code,
      String name,
      String symbol,
      BigDecimal acceptRate,
      BigDecimal adjustRate,
      boolean isStandard,
      boolean disabled) {
    apply(code, name, symbol, acceptRate, adjustRate, isStandard, disabled);
  }

  /**
   * 修改币种
   *
   * @param code 币种代码
   * @param name 币种名称
   * @param symbol 币种符号
   * @param acceptRate 汇率
   * @param adjustRate 调整率
   * @param isStandard 是否本位币
   * @param disabled 是否禁用
   */
  void modify(
      String code,
      String name,
      String symbol,
      BigDecimal acceptRate,
      BigDecimal adjustRate,
      boolean isStandard,
      boolean disabled) {
    apply(code, name, symbol, acceptRate, adjustRate, isStandard, disabled);
  }

  // ==================== 私有：校验与赋值 ====================

  /**
   * 统一的校验与赋值入口（构造器与修改共用）
   *
   * <p>把「去空格 → 非空校验 → 赋值」收敛到一处，避免构造器和 {@link #modify} 各写一遍导致规则漂移。
   *
   * @param code 币种代码
   * @param name 币种名称
   * @param symbol 币种符号
   * @param acceptRate 汇率
   * @param adjustRate 调整率
   * @param isStandard 是否本位币
   * @param disabled 是否禁用
   */
  private void apply(
      String code,
      String name,
      String symbol,
      BigDecimal acceptRate,
      BigDecimal adjustRate,
      boolean isStandard,
      boolean disabled) {
    String trimmedCode = StringUtils.trimToNull(code);
    if (trimmedCode == null) {
      throw new DomainException("币种代码不能为空");
    }
    this.code = trimmedCode;
    this.name = StringUtils.trimToNull(name);
    this.symbol = StringUtils.trimToNull(symbol);
    this.acceptRate = acceptRate;
    this.adjustRate = adjustRate;
    this.isStandard = isStandard;
    this.disabled = disabled;
  }
}
