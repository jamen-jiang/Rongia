package com.rongia.production.domain.bom;

import com.rongia.shared.domain.aggregate.UpdateAggregateRoot;
import com.rongia.shared.domain.exception.DomainException;
import com.rongia.shared.domain.util.EnumUtils;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.SQLRestriction;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 物料清单（BOM）
 *
 * <p>父件与子件都是 SKU，所以 BOM 本质是 SKU 对 SKU 的自关联：成品、毛坯、靶材、包装袋都挂在同一张 Variant 表上，谁有 BOM 谁就能被展开。多层 BOM
 * 不需要额外字段——毛坯 SKU 自己再有一份 BOM 就是三层。
 *
 * <p>同一父件可以挂多条 BOM，靠版本号区分：版本号是配方自己的标识，升版就换一个号，历史工单回溯时按
 * 「父件SKU + 版本号」定位到当时那一份。{@code disabled} 只表示停用（不再进工单选择），与版本无关；
 * 停用的版本照样留着，因为按它下达过的工单还要能连回来。工单下达时取未停用的那一条，并把用到的表头与
 * 用料行快照进工单，之后 BOM 怎么改都不影响在制工单。
 *
 * <p>产出批量：{@code qty} + {@code unitId} 说明这份配方是按「产出这么多父件」定义的，用料行里的
 * 单位用量都是这一批量下的量（照 Odoo 的 {@code product_qty}/{@code product_uom_id}、ERPNext 的
 * {@code qty}/{@code uom}）。工单下达时按「工单数量 ÷ 产出数量」折算，不再假定 1:1。
 *
 * <p>{@code materialId} 是父件所属的 SPU，由调用方随 SKU 一起提交，只当「按系列浏览 BOM」的分组列用。物料类型 不在这里存：它记在 {@code Variant}
 * 主数据上，BOM 再抄一份就成了两处说法，筛选时按 SPU 分组即可。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "Bom",
    indexes = {
      @Index(name = "IX_Bom_MaterialId", columnList = "MaterialId"),
      @Index(name = "IX_Bom_VariantId", columnList = "VariantId"),
      @Index(name = "IX_Bom_Code", columnList = "Code"),
      @Index(name = "IX_Bom_Type", columnList = "Type")
      // 注意：VariantId + Version 的过滤唯一索引带 WHERE IsDeleted=0，JPA 不支持，用 Flyway 创建
    })
public class Bom extends UpdateAggregateRoot {

  /** BOM 类型编码，取值见 {@link BomTypeEnum} */
  @Nationalized
  @Column(name = "Type", nullable = false, length = 50)
  private String type;

  /** 参考号（可空，照 Odoo 的 code；只给人看，不是唯一键） */
  @Nationalized
  @Column(name = "Code", length = 100)
  @Nullable
  private String code;

  /** 版本号：与父件SKU共同构成唯一键，升版就是换一个号 */
  @Nationalized
  @Column(name = "Version", nullable = false, length = 50)
  private String version;

  /** 父件SPU（按系列查 BOM 用） */
  @Column(name = "MaterialId", nullable = false)
  private Long materialId;

  /** 父件SKU（可空，照 Odoo 的 product_id）：为空表示整系列共用，填了表示这个变体专用 */
  @Column(name = "VariantId")
  @Nullable
  private Long variantId;

  /** 工艺路线Id（可空，照 ERPNext 的 BOM.routing）：这张 BOM 下达时用哪条路线，空表示由工单指定 */
  @Column(name = "RoutingId")
  @Nullable
  private Long routingId;

  /** 产出数量：用料行里的单位用量都按这一批量计（照 Odoo 的 product_qty、ERPNext 的 qty） */
  @Column(name = "Qty", nullable = false, precision = 18, scale = 2)
  private BigDecimal qty;

  /** 产出数量对应的单位Id（照 Odoo 的 product_uom_id） */
  @Column(name = "UnitId", nullable = false)
  private Long unitId;

  /** 开工方式编码，取值见 {@link BomStartModeEnum} */
  @Nationalized
  @Column(name = "StartMode", nullable = false, length = 50)
  private String startMode;

  /** 领料方式编码，取值见 {@link BomIssueModeEnum} */
  @Nationalized
  @Column(name = "IssueMode", nullable = false, length = 50)
  private String issueMode;

  /**
   * 制造天数：从下达工单那天算起，把货做出来平均要多少天。
   *
   * <p>排产时「最晚哪天下单」就按它倒推：想要 20 号交货、做要 5 天，19 号之后下单就来不及了。
   */
  @Column(name = "LeadDays", nullable = false)
  private Integer leadDays = 0;

  /**
   * 备料天数：比开工日再往前推这么多天，先把工单建出来、把料备齐。
   *
   * <p>料没到位机器开不了——做要 5 天、料还得 3 天，那就提前 3 天建单领料，别等到开工那天才发现料没到。
   */
  @Column(name = "PrepareDays", nullable = false)
  private Integer prepareDays = 0;

  /** 自动生成工单的批量（可空，空表示不按批量；照 Odoo 的 batch_size） */
  @Column(name = "BatchSize", precision = 18, scale = 2)
  @Nullable
  private BigDecimal batchSize;

  /** 是否禁用：禁用后下达工单不再选它，已按它下达的工单不受影响 */
  @Column(name = "Disabled", nullable = false)
  private Boolean disabled = false;

  /** 备注（可空） */
  @Nationalized
  @Column(name = "Remark", length = 500)
  @Nullable
  private String remark;

  /** 用料明细 */
  @OneToMany(
      fetch = FetchType.EAGER,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE},
      orphanRemoval = true)
  @JoinColumn(name = "BomId")
  @SQLRestriction("IsDeleted = 0")
  private List<BomLine> bomLines = new ArrayList<>();

  /**
   * 获取用料明细（只读）
   *
   * @return 不可变列表
   */
  public List<BomLine> getBomLines() {
    return Collections.unmodifiableList(bomLines);
  }

  /**
   * 构造器
   *
   * @param type BOM 类型编码
   * @param code 参考号（可空）
   * @param version 版本号
   * @param materialId 父件SPU
   * @param variantId 父件SKU（可空）
   * @param routingId 工艺路线Id（可空）
   * @param qty 产出数量
   * @param unitId 产出单位Id
   * @param startMode 开工方式编码
   * @param issueMode 领料方式编码
   * @param leadDays 制造天数：从下达算起把货做出来平均要多少天
   * @param prepareDays 备料天数：比开工日再往前推这么多天建单备料
   * @param batchSize 自动生成工单的批量（可空）
   * @param disabled 是否禁用
   * @param remark 备注（可空）
   */
  Bom(
      String type,
      @Nullable String code,
      String version,
      long materialId,
      @Nullable Long variantId,
      @Nullable Long routingId,
      BigDecimal qty,
      long unitId,
      String startMode,
      String issueMode,
      int leadDays,
      int prepareDays,
      @Nullable BigDecimal batchSize,
      boolean disabled,
      @Nullable String remark) {
    apply(
        type,
        code,
        version,
        materialId,
        variantId,
        routingId,
        qty,
        unitId,
        startMode,
        issueMode,
        leadDays,
        prepareDays,
        batchSize,
        disabled,
        remark);
  }

  /**
   * 修改 BOM 表头
   *
   * @param type BOM 类型编码
   * @param code 参考号（可空）
   * @param version 版本号
   * @param materialId 父件SPU
   * @param variantId 父件SKU（可空）
   * @param routingId 工艺路线Id（可空）
   * @param qty 产出数量
   * @param unitId 产出单位Id
   * @param startMode 开工方式编码
   * @param issueMode 领料方式编码
   * @param leadDays 制造天数：从下达算起把货做出来平均要多少天
   * @param prepareDays 备料天数：比开工日再往前推这么多天建单备料
   * @param batchSize 自动生成工单的批量（可空）
   * @param disabled 是否禁用
   * @param remark 备注（可空）
   */
  void modify(
      String type,
      @Nullable String code,
      String version,
      long materialId,
      @Nullable Long variantId,
      @Nullable Long routingId,
      BigDecimal qty,
      long unitId,
      String startMode,
      String issueMode,
      int leadDays,
      int prepareDays,
      @Nullable BigDecimal batchSize,
      boolean disabled,
      @Nullable String remark) {
    apply(
        type,
        code,
        version,
        materialId,
        variantId,
        routingId,
        qty,
        unitId,
        startMode,
        issueMode,
        leadDays,
        prepareDays,
        batchSize,
        disabled,
        remark);
  }

  /**
   * 添加用料行
   *
   * @param materialId 子件SPU
   * @param variantId 子件SKU
   * @param routingOperationId 消耗的路线工序Id（可空）
   * @param qty 单位用量
   * @param lossRate 损耗率（百分比）
   * @param unitId 单位Id
   * @param sequence 行号
   * @throws DomainException 行号或子件重复时抛出
   */
  public void addLine(
      long materialId,
      long variantId,
      @Nullable Long routingOperationId,
      BigDecimal qty,
      BigDecimal lossRate,
      long unitId,
      int sequence) {
    if (isExistSequence(sequence, null)) {
      throw new DomainException("BOM 行号" + sequence + "已存在!");
    }
    if (isExistComponent(variantId, null)) {
      throw new DomainException("子件已存在于当前 BOM 中!");
    }
    bomLines.add(
        new BomLine(materialId, variantId, routingOperationId, qty, lossRate, unitId, sequence));
  }

  /**
   * 修改用料行
   *
   * @param bomLineId BOM 行Id
   * @param materialId 子件SPU
   * @param variantId 子件SKU
   * @param routingOperationId 消耗的路线工序Id（可空）
   * @param qty 单位用量
   * @param lossRate 损耗率（百分比）
   * @param unitId 单位Id
   * @param sequence 行号
   * @throws DomainException 行不存在、行号或子件重复时抛出
   */
  public void modifyLine(
      long bomLineId,
      long materialId,
      long variantId,
      @Nullable Long routingOperationId,
      BigDecimal qty,
      BigDecimal lossRate,
      long unitId,
      int sequence) {
    BomLine target = findLine(bomLineId);
    if (target == null) {
      throw new DomainException("BOM 行不存在!");
    }
    if (isExistSequence(sequence, bomLineId)) {
      throw new DomainException("BOM 行号" + sequence + "已存在!");
    }
    if (isExistComponent(variantId, bomLineId)) {
      throw new DomainException("子件已存在于当前 BOM 中!");
    }
    target.modify(materialId, variantId, routingOperationId, qty, lossRate, unitId, sequence);
  }

  /**
   * 删除用料行（逻辑删除）
   *
   * @param bomLineId BOM 行Id
   */
  public void deleteLine(long bomLineId) {
    BomLine target = findLine(bomLineId);
    if (target == null) {
      return;
    }
    target.delete();
  }

  /**
   * 按行Id查找未删除的用料行
   *
   * @param bomLineId BOM 行Id
   * @return 匹配到的用料行，找不到返回 {@code null}
   */
  @Nullable
  private BomLine findLine(long bomLineId) {
    return bomLines.stream()
        .filter(i -> Objects.equals(i.getId(), bomLineId) && !i.getIsDeleted())
        .findFirst()
        .orElse(null);
  }

  // ==================== 私有：校验与赋值 ====================

  /**
   * 统一的校验与赋值入口（构造器与修改共用）
   *
   * @param type BOM 类型编码
   * @param code 参考号（可空）
   * @param version 版本号
   * @param materialId 父件SPU
   * @param variantId 父件SKU（可空）
   * @param routingId 工艺路线Id（可空）
   * @param qty 产出数量
   * @param unitId 产出单位Id
   * @param startMode 开工方式编码
   * @param issueMode 领料方式编码
   * @param leadDays 制造天数：从下达算起把货做出来平均要多少天
   * @param prepareDays 备料天数：比开工日再往前推这么多天建单备料
   * @param batchSize 自动生成工单的批量（可空）
   * @param disabled 是否禁用
   * @param remark 备注（可空）
   * @throws DomainException SPU、SKU、产出数量、单位、版本为空、类型/开工/领料编码非法或天数为负时抛出
   */
  private void apply(
      String type,
      @Nullable String code,
      String version,
      long materialId,
      @Nullable Long variantId,
      @Nullable Long routingId,
      BigDecimal qty,
      long unitId,
      String startMode,
      String issueMode,
      int leadDays,
      int prepareDays,
      @Nullable BigDecimal batchSize,
      boolean disabled,
      @Nullable String remark) {
    if (materialId <= 0) {
      throw new DomainException("父件SPU不能为空");
    }
    if (variantId != null && variantId <= 0) {
      throw new DomainException("父件不能为空");
    }
    if (routingId != null && routingId <= 0) {
      throw new DomainException("工艺路线不能为空");
    }
    if (qty.signum() <= 0) {
      throw new DomainException("产出数量必须大于0");
    }
    if (unitId <= 0) {
      throw new DomainException("产出单位不能为空");
    }
    String trimmedVersion = StringUtils.trimToNull(version);
    if (trimmedVersion == null) {
      throw new DomainException("版本号不能为空");
    }
    this.type = EnumUtils.getCode(BomTypeEnum.class, type, "BOM 类型");
    this.startMode = EnumUtils.getCode(BomStartModeEnum.class, startMode, "开工方式");
    this.issueMode = EnumUtils.getCode(BomIssueModeEnum.class, issueMode, "领料方式");
    if (leadDays < 0) {
      throw new DomainException("制造天数不能为负数");
    }
    if (prepareDays < 0) {
      throw new DomainException("备料天数不能为负数");
    }
    if (batchSize != null && batchSize.signum() <= 0) {
      throw new DomainException("批量必须大于0");
    }

    this.code = StringUtils.trimToNull(code);
    this.version = trimmedVersion;
    this.materialId = materialId;
    this.variantId = variantId;
    this.routingId = routingId;
    this.qty = qty;
    this.unitId = unitId;
    this.leadDays = leadDays;
    this.prepareDays = prepareDays;
    this.batchSize = batchSize;
    this.disabled = disabled;
    this.remark = remark;
  }

  /**
   * 判断行号是否已被占用
   *
   * <p>行号在单张 BOM 内必须唯一；修改时需排除自身，否则会把「保持原行号」误判为冲突。
   *
   * @param sequence 待校验的行号
   * @param excludeId 需要排除的 BOM 行Id（新增时传 {@code null}）
   * @return true：已被其它未删除的行占用
   */
  private boolean isExistSequence(int sequence, @Nullable Long excludeId) {
    return bomLines.stream()
        .filter(i -> !i.getIsDeleted())
        .filter(i -> !Objects.equals(i.getId(), excludeId))
        .anyMatch(i -> Objects.equals(i.getSequence(), sequence));
  }

  /**
   * 判断子件是否已存在于当前 BOM
   *
   * <p>同一子件在同一张 BOM 里只能出现一次，否则展开成工单用料会出现两条同 SKU 的记录，领料时无法区分。
   *
   * @param variantId 待校验的子件SKU
   * @param excludeId 需要排除的 BOM 行Id（新增时传 {@code null}）
   * @return true：已存在
   */
  private boolean isExistComponent(long variantId, @Nullable Long excludeId) {
    return bomLines.stream()
        .filter(i -> !i.getIsDeleted())
        .filter(i -> !Objects.equals(i.getId(), excludeId))
        .anyMatch(i -> Objects.equals(i.getVariantId(), variantId));
  }
}
