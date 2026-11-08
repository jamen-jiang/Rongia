package com.rongia.system.domain.serialnumber;

import com.rongia.shared.domain.aggregate.UpdateAggregateRoot;
import com.rongia.shared.domain.enums.DateTypeEnum;
import com.rongia.shared.domain.enums.SerialNumberTypeEnum;
import com.rongia.shared.domain.exception.DomainException;
import com.rongia.shared.domain.util.EnumUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Nationalized;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 流水号规则
 *
 * <p>字段照 Odoo {@code ir.sequence} 取：{@code prefix}/{@code suffix} 模板（支持日期插值，见
 * {@link SerialNumberRuleDomainService}）、{@code pad} 补零位数、{@code number} 当前号
 * （Odoo 的 {@code number_next_actual}）、{@code step} 步长。
 *
 * <p>类型对应 {@link SerialNumberTypeEnum#getCode()}（如 {@code SalesOrder}），一张表一个类型，表里的
 * 红字单、普通单共用一个号段；单据落库时由监听器按类型取号（{@code SerialNumberRuleEventHandler}），
 * 没配规则的类型直接报错——和 Odoo {@code next_by_code} 找不到序列时抛错一致。
 *
 * <p>取号并发控制：计数在业务事务内用悲观锁行推进（锁到事务提交，回滚归还号，单号严格连续无缺口）；
 * 配了重置周期（{@code rule}，{@link DateTypeEnum} 年/月/日）时，跨期的第一次取号把计数归 1。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "SerialNumberRule",
    indexes = {
      @Index(name = "IX_SerialNumberRule_Type", columnList = "Type")
      // 注意：Type 的过滤唯一索引带 WHERE IsDeleted=0，用 Flyway 创建
    })
public class SerialNumberRule extends UpdateAggregateRoot {

  /** 类型（对应流水号类型编码，如 SalesOrder） */
  @Nationalized
  @Column(name = "Type", nullable = false, length = 50)
  private String type;

  /**
   * 计数重置周期（可空=永不重置，取 {@link DateTypeEnum} 编码：Year/Month/Day）
   *
   * <p>配了周期时，跨期的第一次取号把计数归 1（判定在行锁内完成，并发安全）；前缀必须带上对应
   * 粒度的日期变量（如按日重置配 {@code %(day)s}），否则不同周期的号会撞车。
   */
  @Nationalized
  @Column(name = "Rule", length = 20)
  @Nullable
  private String rule;

  /**
   * 前缀模板（空串=无前缀）
   *
   * <p>支持日期插值（Odoo 同款写法）：{@code %(year)s}（四位年）、{@code %(month)s}（两位月）、
   * {@code %(day)s}（两位日）、{@code %(h24)s}（24 小时）、{@code %(min)s}（分）、{@code %(sec)s}（秒）、
   * {@code %(doy)s}（年内的第几天）、{@code %(woy)s}（年内的第几周）。如 {@code SO%(year)s-}。
   */
  @Nationalized
  @Column(name = "Prefix", length = 100)
  private String prefix;

  /** 后缀模板（空串=无后缀，支持与前缀相同的日期插值） */
  @Nationalized
  @Column(name = "Suffix", length = 50)
  private String suffix;

  /** 补零位数（0 表示不补零，如 4 → 0001） */
  @Column(name = "Pad", nullable = false)
  private Integer pad;

  /** 当前号（本次要发出去的号，取号后按步长推进） */
  @Column(name = "Number", nullable = false)
  private Integer number = 1;

  /** 步长（每取一次号计数推进多少，一般为 1） */
  @Column(name = "Step", nullable = false)
  private Integer step = 1;

  /**
   * 最近一次取号时间（配合 {@link #rule} 判定是否跨期）
   *
   * <p>还没取过号时为 null（{@link DateTypeEnum#inSamePeriod} 把 null 视为跨期，首取即触发归 1）。
   */
  @Column(name = "LastNumberTime")
  @Nullable
  private LocalDateTime lastNumberTime;

  /** 备注（可空） */
  @Nationalized
  @Column(name = "Remark", length = 500)
  @Nullable
  private String remark;

  /**
   * 构造器
   *
   * @param type 类型（对应流水号类型编码，如 SalesOrder）
   * @param rule 计数重置周期（可空=永不重置，Year/Month/Day）
   * @param prefix 前缀模板（空串=无前缀）
   * @param suffix 后缀模板（空串=无后缀）
   * @param pad 补零位数
   * @param step 步长
   * @param remark 备注（可空）
   */
  SerialNumberRule(
      String type,
      @Nullable String rule,
      String prefix,
      String suffix,
      int pad,
      int step,
      @Nullable String remark) {
    apply(type, rule, prefix, suffix, pad, step, remark);
    this.number = 1;
  }

  /**
   * 修改规则
   *
   * @param type 类型（对应流水号类型编码，如 SalesOrder）
   * @param rule 计数重置周期（可空=永不重置，Year/Month/Day）
   * @param prefix 前缀模板（空串=无前缀）
   * @param suffix 后缀模板（空串=无后缀）
   * @param pad 补零位数
   * @param step 步长
   * @param remark 备注（可空）
   */
  public void modify(
      String type,
      @Nullable String rule,
      String prefix,
      String suffix,
      int pad,
      int step,
      @Nullable String remark) {
    apply(type, rule, prefix, suffix, pad, step, remark);
  }

  /**
   * 消费一个号：返回当前号并把计数按步长推进
   *
   * <p>由仓储在行锁内调用（悲观锁锁到业务事务提交），保证并发取号不重不漏。配了重置周期
   * （{@link #rule}）且本次取号跨期时，先把计数归 1 再消费——判定与推进同在行锁内，并发安全。
   *
   * @param now 本次取号时间
   * @return 本次使用的号
   */
  public int advance(LocalDateTime now) {
    if (this.rule != null) {
      DateTypeEnum period = EnumUtils.fromCode(DateTypeEnum.class, this.rule);
      if (period != null && !period.inSamePeriod(this.lastNumberTime, now)) {
        this.number = 1;
      }
    }
    this.lastNumberTime = now;
    int number = this.number;
    this.number = number + this.step;
    return number;
  }

  // ==================== 私有：校验与赋值 ====================

  /**
   * 统一的校验与赋值入口（构造器与修改共用）
   *
   * <p>当前号（{@link #number}）不在这里：构造器取字段默认值 1，修改不允许动它——号是流水记录，
   * 只由 {@link #advance} 在取号时推进。
   *
   * @param type 类型（对应流水号类型编码，如 SalesOrder）
   * @param rule 计数重置周期（可空=永不重置，Year/Month/Day）
   * @param prefix 前缀模板（空串=无前缀）
   * @param suffix 后缀模板（空串=无后缀）
   * @param pad 补零位数
   * @param step 步长
   * @param remark 备注（可空）
   * @throws DomainException 类型为空、重置周期/补零位数/步长有误或重置周期与前缀不匹配时抛出
   */
  private void apply(
      String type,
      @Nullable String rule,
      String prefix,
      String suffix,
      int pad,
      int step,
      @Nullable String remark) {
    String trimmedType = StringUtils.trimToNull(type);
    if (trimmedType == null) {
      throw new DomainException("流水号规则类型不能为空");
    }
    String trimmedRule = StringUtils.trimToNull(rule);
    if (trimmedRule != null) {
      DateTypeEnum period = EnumUtils.fromCode(DateTypeEnum.class, trimmedRule);
      if (period == null) {
        throw new DomainException("流水号规则的重置周期" + trimmedRule + "有误!");
      }
      String token = period.requiredToken();
      if (!prefix.contains("%(" + token + ")s")) {
        throw new DomainException(
            "流水号规则按"
                + period.getLabel()
                + "重置时，前缀必须包含%("
                + token
                + ")s，否则不同周期的号会重复");
      }
    }
    if (pad < 0 || pad > 20) {
      throw new DomainException("流水号规则的补零位数" + pad + "有误!");
    }
    if (step <= 0) {
      throw new DomainException("流水号规则的步长必须大于0");
    }

    this.type = trimmedType;
    this.rule = trimmedRule;
    this.prefix = StringUtils.trimToEmpty(prefix);
    this.suffix = StringUtils.trimToEmpty(suffix);
    this.pad = pad;
    this.step = step;
    this.remark = remark;
  }
}
