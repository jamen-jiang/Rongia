package com.rongia.basedata.domain.material;

import com.rongia.shared.domain.aggregate.UpdateAggregateRoot;
import com.rongia.shared.domain.exception.DomainException;
import com.rongia.shared.domain.util.EnumUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 物料
 *
 * <p>这里只放「物料是什么」：分类、代码、名称、基本单位。采购价、采购单位、币种、起订量不在这里——那是 「供应商 ×
 * 物料」这一层的事，一个物料可以有多个供应商和多个报价，钉在物料上只能留下最新一个值，历史价当场丢失。见 {@code
 * com.rongia.basedata.domain.suppliermaterial.SupplierMaterial}。按什么单位进货本来就随供应商变 （这家按卷、那家按米），Odoo 19
 * 也把采购单位从产品表挪到了供应商价格表。
 *
 * <p>{@code categoryId} 说「归哪一类、挂哪组规格属性」，跟「这张料怎么来、要不要管库存」是两回事，不能并成一个枚举—— 同一分类下的东西完全可以既自制又外购。
 *
 * <p>后一件事按 Odoo 产品表的形状存：一个物料类型（库存料、消耗品、服务，对应它的 {@code detailed_type}，
 * 要不要管库存就分在这三档里，不单开一个"是否维护库存"的布尔）加两个"能不能"（允许采购 {@code purchase_ok}、 允许销售 {@code
 * sale_ok}）。自制还是外购<em>不在这里落列</em>：Odoo 用产品上的路线（采购、制造、分包）表达，
 * 我们对应「有没有工艺路线」和「有没有采购价格」，两个都成立就是既买又做。整件委外照 Odoo 也是路线的事； 单道工序外发另说，在路线的工序行上。固定资产照 Odoo
 * 不落物料表——资产是会计侧从账单行生成的， 办公用品、劳保、滤芯这类登记成消耗品就够了。
 *
 * <p>税率成对存：进项给采购单据取默认值、销项给销售单据取默认值，两家国产 ERP 都是两条——用友存货档案上的 「销项税率%」与「进项税率%」，金蝶 K/3
 * 物料上一个税率加星辰商品资料上的进项/销项两列；Odoo 也是客户税与供应商税 两个字段，SAP 在销售视图和采购视图各维护一个税分类。一条税率列说不清指的是买还是卖：外购树脂粉进项 13%、
 * 卖镜片销项 13%、找外协镀膜进项可能 6%。两列都只是<em>默认值</em>，单据行上照别家一样可以再改；存的是税率Id 而不是百分比数字，税率从 13% 调整成 11%
 * 时改一处，不必批量批改几百条物料。列名用采购/销售而不是 input/output， 是为了不跟「输入/输出」混。供应商上那条税率不跟着拆：供应商只出现在采购方向，本来就是进项。
 *
 * <p>物料上填的属性值放通用赋值表 {@code AttributeAssignment}（{@code EntityType = 'Material'}），不存 JSON
 * 列：一行一个值才能按代码查、按值排序，也能让同一代码有多个值（比如颜色）。定义在 {@code Attribute}， 赋值在 {@code AttributeAssignment}，相当于
 * SAP 把 {@code CABN}（定义）和 {@code AUSP}（赋值， 带 {@code OBJ_TYPE} + {@code OBJ_ID}）分在两张表。这张赋值表已挪到平台层
 * {@code com.rongia.system.domain.attribute}，物料聚合上的挂载暂时停用，见字段处注释。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "Material",
    indexes = {
      @Index(name = "IX_Material_CategoryId", columnList = "CategoryId"),
      @Index(name = "IX_Material_Type", columnList = "Type"),
      @Index(name = "IX_Material_Code", columnList = "Code"),
      @Index(name = "IX_Material_Name", columnList = "Name")
      // 注意：Code 的过滤唯一索引带 WHERE IsDeleted=0，用 Flyway 创建
    })
public class Material extends UpdateAggregateRoot {

  /** 所属分类Id */
  @Column(name = "CategoryId", nullable = false)
  private Long categoryId;

  /** 物料类型编码，取值见 {@link MaterialTypeEnum} */
  @Nationalized
  @Column(name = "Type", nullable = false, length = 50)
  private String type;

  /** 物料代码 */
  @Nationalized
  @Column(name = "Code", nullable = false, length = 100)
  private String code;

  /** 物料名称 */
  @Nationalized
  @Column(name = "Name", nullable = false, length = 200)
  private String name;

  /** 物料描述（可空） */
  @Nationalized
  @Column(name = "Description", length = 200)
  @Nullable
  private String description;

  /** 销售价（可空）：销售单据取用的默认价，照 Odoo 的 list_price */
  @Column(name = "SalesPrice", precision = 18, scale = 2)
  @Nullable
  private BigDecimal salesPrice;

  /** 成本（可空）：照 Odoo 的 standard_price */
  @Column(name = "Cost", precision = 18, scale = 2)
  @Nullable
  private BigDecimal cost;

  /** 基本单位Id */
  @Column(name = "UnitId", nullable = false)
  private Long unitId;

  /** 是否允许采购 */
  @Column(name = "IsPurchaseable", nullable = false)
  private Boolean isPurchaseable = true;

  /** 是否允许销售 */
  @Column(name = "IsSaleable", nullable = false)
  private Boolean isSaleable = true;

  /** 进项税率Id（可空）：采购单据取用的默认税率 */
  @Column(name = "PurchaseTaxRateId")
  @Nullable
  private Long purchaseTaxRateId;

  /** 销项税率Id（可空）：销售单据取用的默认税率 */
  @Column(name = "SalesTaxRateId")
  @Nullable
  private Long salesTaxRateId;

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
   * @param categoryId 所属分类Id
   * @param type 物料类型
   * @param code 物料代码
   * @param name 物料名称
   * @param description 物料描述（可空）
   * @param salesPrice 销售价（可空）
   * @param cost 成本（可空）
   * @param unitId 基本单位Id
   * @param isPurchaseable 是否允许采购
   * @param isSaleable 是否允许销售
   * @param purchaseTaxRateId 进项税率Id（可空）
   * @param salesTaxRateId 销项税率Id（可空）
   * @param disabled 是否禁用
   * @param remark 备注（可空）
   * @param attachmentIds 附件Id集合（可空）
   */
  Material(
      long categoryId,
      String type,
      String code,
      String name,
      @Nullable String description,      @Nullable BigDecimal salesPrice,
      @Nullable BigDecimal cost,
      long unitId,
      boolean isPurchaseable,
      boolean isSaleable,
      @Nullable Long purchaseTaxRateId,
      @Nullable Long salesTaxRateId,
      boolean disabled,
      @Nullable String remark,
      @Nullable List<Long> attachmentIds) {
    apply(
        categoryId,
        type,
        code,
        name,
        description,        salesPrice,
        cost,        unitId,
        isPurchaseable,
        isSaleable,
        purchaseTaxRateId,
        salesTaxRateId,
        disabled,
        remark);
    addDomainEvent(new MaterialSavedEvent(this, attachmentIds));
  }

  /**
   * 修改
   *
   * @param categoryId 所属分类Id
   * @param type 物料类型
   * @param code 物料代码
   * @param name 物料名称
   * @param description 物料描述（可空）
   * @param salesPrice 销售价（可空）
   * @param cost 成本（可空）
   * @param unitId 单位Id
   * @param isPurchaseable 是否允许采购
   * @param isSaleable 是否允许销售
   * @param purchaseTaxRateId 进项税率Id（可空）
   * @param salesTaxRateId 销项税率Id（可空）
   * @param disabled 是否禁用
   * @param remark 备注（可空）
   * @param attachmentIds 附件Id集合（可空）
   */
  void modify(
      long categoryId,
      String type,
      String code,
      String name,
      @Nullable String description,      @Nullable BigDecimal salesPrice,
      @Nullable BigDecimal cost,
      long unitId,
      boolean isPurchaseable,
      boolean isSaleable,
      @Nullable Long purchaseTaxRateId,
      @Nullable Long salesTaxRateId,
      boolean disabled,
      @Nullable String remark,
      @Nullable List<Long> attachmentIds) {
    apply(
        categoryId,
        type,
        code,
        name,
        description,        salesPrice,
        cost,        unitId,
        isPurchaseable,
        isSaleable,
        purchaseTaxRateId,
        salesTaxRateId,
        disabled,
        remark);
    addDomainEvent(new MaterialSavedEvent(this, attachmentIds));
  }

  /** 删除 */
  @Override
  public void delete() {
    super.delete();
    addDomainEvent(new MaterialDeletedEvent(this));
  }

  // ==================== 私有：校验与赋值 ====================

  /**
   * 统一的校验与赋值入口（构造器与修改共用）
   *
   * <p>把「去空格 → 校验 → 赋值」收敛到一处，避免构造器和 {@link #modify} 各写一遍导致规则漂移。
   *
   * @param categoryId 所属分类Id
   * @param type 物料类型
   * @param code 物料代码
   * @param name 物料名称
   * @param description 物料描述（可空）
   * @param salesPrice 销售价（可空）
   * @param cost 成本（可空）
   * @param unitId 基本单位Id
   * @param isPurchaseable 是否允许采购
   * @param isSaleable 是否允许销售
   * @param purchaseTaxRateId 进项税率Id（可空）
   * @param salesTaxRateId 销项税率Id（可空）
   * @param disabled 是否禁用
   * @param remark 备注（可空）
   */
  private void apply(
      long categoryId,
      String type,
      String code,
      String name,
      @Nullable String description,      @Nullable BigDecimal salesPrice,
      @Nullable BigDecimal cost,
      long unitId,
      boolean isPurchaseable,
      boolean isSaleable,
      @Nullable Long purchaseTaxRateId,
      @Nullable Long salesTaxRateId,
      boolean disabled,
      @Nullable String remark) {
    if (categoryId <= 0) {
      throw new DomainException("物料分类不能为空");
    }
    this.type = EnumUtils.getCode(MaterialTypeEnum.class, type, "物料类型");
    if (salesPrice != null && salesPrice.signum() < 0) {
      throw new DomainException("销售价不能为负数");
    }
    if (cost != null && cost.signum() < 0) {
      throw new DomainException("成本不能为负数");
    }

    this.categoryId = categoryId;
    this.code = code;
    this.name = name;
    this.description = description;    this.salesPrice = salesPrice;
    this.cost = cost;
    this.unitId = unitId;
    this.isPurchaseable = isPurchaseable;
    this.isSaleable = isSaleable;
    this.purchaseTaxRateId = purchaseTaxRateId;
    this.salesTaxRateId = salesTaxRateId;
    this.disabled = disabled;
    this.remark = remark;
  }
}
