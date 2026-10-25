package com.rongia.basedata.domain.unit;

import com.rongia.shared.domain.aggregate.UpdateAggregateRoot;
import com.rongia.shared.domain.entity.Parent;
import com.rongia.shared.domain.exception.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Nationalized;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 单位
 *
 * <p>单位自己是一棵树：每个单位只认一个「参考单位」，再填一个「包含几个参考单位」。克挂在千克下面填
 * 0.001，毫克挂在克下面填 0.001，逐级往上乘出绝对系数 {@code factor}。换算就一句：
 * {@code 数量 × 来源单位系数 ÷ 目标单位系数}。
 *
 * <p>没有「单位组」：树的根自己就是一组量纲。两棵不同的树之间不能换算（米和千克没有共同根），
 * 这比给每个单位再挂一个分类列更省事，也不会出现「分类改成重量、换算率却指向长度」这种前后矛盾。
 *
 * <p>{@code factor} 与 {@code parentPath} 都是算出来的：改父单位或换算值时，本单位和它下面所有子孙都要跟着重算，
 * 由 {@link UnitDomainService} 统一编排。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "Unit",
    indexes = {
      @Index(name = "IX_Unit_ParentId", columnList = "ParentId"),
      @Index(name = "IX_Unit_ParentPath", columnList = "ParentPath"),
      @Index(name = "IX_Unit_Code", columnList = "Code"),
      @Index(name = "IX_Unit_Name", columnList = "Name"),
      @Index(name = "IX_Unit_Sequence", columnList = "Sequence")
      // 注意：Code 的过滤唯一索引带 WHERE IsDeleted=0，JPA 不支持，用 Flyway 创建
    })
public class Unit extends UpdateAggregateRoot implements Parent {

  /** 中间计算保留的小数位 */
  private static final int CALC_SCALE = 12;

  /** 落数前归一「数量 ÷ 步长」的小数位，用来吃掉换算率的末位误差 */
  private static final int SNAP_SCALE = 6;

  /** 参考单位Id（可空，空表示这是一棵树的根） */
  @Column(name = "ParentId")
  @Nullable
  private Long parentId;

  /** 层级路径（可空，只存祖先、不含自己，形如 "1-3-5"） */
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

  /** 包含几个参考单位（根单位即它自己的绝对系数），必须大于零 */
  @Column(name = "RelativeFactor", nullable = false, precision = 28, scale = 12)
  private BigDecimal relativeFactor;

  /** 折成根单位的绝对系数（算出来落库，改父或换算值时由领域服务重算） */
  @Column(name = "Factor", nullable = false, precision = 28, scale = 12)
  private BigDecimal factor;

  /** 数量落数步长：换算结果必须是它的整数倍（件填 1，按 0.01 计量的填 0.01） */
  @Column(name = "Rounding", nullable = false, precision = 28, scale = 12)
  private BigDecimal rounding;

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
   * @param parentId 参考单位Id（可空，空表示根）
   * @param parentPath 层级路径（可空）
   * @param code 代码
   * @param name 名称
   * @param relativeFactor 包含几个参考单位
   * @param parentFactor 参考单位的绝对系数（可空，根单位传 null）
   * @param rounding 数量落数步长
   * @param disabled 是否禁用
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   */
  Unit(
      @Nullable Long parentId,
      @Nullable String parentPath,
      String code,
      String name,
      BigDecimal relativeFactor,
      @Nullable BigDecimal parentFactor,
      BigDecimal rounding,
      boolean disabled,
      @Nullable Integer sequence,
      @Nullable String remark) {
    apply(
        parentId,
        parentPath,
        code,
        name,
        relativeFactor,
        parentFactor,
        rounding,
        disabled,
        sequence,
        remark);
  }

  /**
   * 修改单位
   *
   * @param parentId 参考单位Id（可空，空表示根）
   * @param parentPath 层级路径（可空）
   * @param code 代码
   * @param name 名称
   * @param relativeFactor 包含几个参考单位
   * @param parentFactor 参考单位的绝对系数（可空，根单位传 null）
   * @param rounding 数量落数步长
   * @param disabled 是否禁用
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   */
  void modify(
      @Nullable Long parentId,
      @Nullable String parentPath,
      String code,
      String name,
      BigDecimal relativeFactor,
      @Nullable BigDecimal parentFactor,
      BigDecimal rounding,
      boolean disabled,
      @Nullable Integer sequence,
      @Nullable String remark) {
    apply(
        parentId,
        parentPath,
        code,
        name,
        relativeFactor,
        parentFactor,
        rounding,
        disabled,
        sequence,
        remark);
  }

  /** 重设层级路径（由领域服务按父节点算好后回填） */
  @Override
  public void changeParentPath(@Nullable String parentPath) {
    this.parentPath = parentPath;
  }

  /**
   * 用新的参考单位系数重算绝对系数
   *
   * <p>只算自己这一层；子孙由领域服务按顺序往下调。
   *
   * @param parentFactor 参考单位的绝对系数（可空，根单位传 null）
   */
  void recalculate(@Nullable BigDecimal parentFactor) {
    this.factor =
        parentFactor == null ? this.relativeFactor : this.relativeFactor.multiply(parentFactor);
  }

  /**
   * 按本单位的步长给数量落数
   *
   * <p>先把「数量 ÷ 步长」在 6 位小数上四舍五入再进位，吃掉换算率的末位误差：1 根 = 1/3 公斤只能存近似值，
   * 3 根折出来是 0.999999999999，直接进位就成了 1.01。方向固定进位（对应 Odoo 的默认
   * {@code rounding_method='UP'}）。
   *
   * @param qty 数量
   * @return 落数后的数量
   */
  public BigDecimal roundQty(BigDecimal qty) {
    BigDecimal steps =
        qty
            .divide(this.rounding, SNAP_SCALE, RoundingMode.HALF_UP)
            .setScale(0, RoundingMode.UP);
    return steps.multiply(this.rounding);
  }

  // ==================== 私有：校验与赋值 ====================

  /**
   * 统一的校验与赋值入口（构造器与修改共用）
   *
   * @param parentId 参考单位Id（可空，空表示根）
   * @param parentPath 层级路径（可空）
   * @param code 代码
   * @param name 名称
   * @param relativeFactor 包含几个参考单位
   * @param parentFactor 参考单位的绝对系数（可空，根单位传 null）
   * @param rounding 数量落数步长
   * @param disabled 是否禁用
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   * @throws DomainException 代码或名称为空、换算值或步长不为正时抛出
   */
  private void apply(
      @Nullable Long parentId,
      @Nullable String parentPath,
      String code,
      String name,
      BigDecimal relativeFactor,
      @Nullable BigDecimal parentFactor,
      BigDecimal rounding,
      boolean disabled,
      @Nullable Integer sequence,
      @Nullable String remark) {
    String trimmedCode = StringUtils.trimToNull(code);
    String trimmedName = StringUtils.trimToNull(name);
    if (trimmedCode == null) {
      throw new DomainException("单位代码不能为空");
    }
    if (trimmedName == null) {
      throw new DomainException("单位名称不能为空");
    }
    if (relativeFactor.signum() <= 0) {
      throw new DomainException("单位" + trimmedCode + "的换算值必须大于零");
    }
    if (rounding.signum() <= 0) {
      throw new DomainException("单位" + trimmedCode + "的落数步长必须大于零");
    }
    if (parentId != null && parentId <= 0) {
      throw new DomainException("参考单位" + parentId + "有误!");
    }

    this.parentId = parentId;
    this.parentPath = parentPath;
    this.code = trimmedCode;
    this.name = trimmedName;
    this.relativeFactor = relativeFactor;
    this.factor = parentFactor == null ? relativeFactor : relativeFactor.multiply(parentFactor);
    this.rounding = rounding;
    this.disabled = disabled;
    this.sequence = sequence;
    this.remark = StringUtils.trimToNull(remark);
  }
}
