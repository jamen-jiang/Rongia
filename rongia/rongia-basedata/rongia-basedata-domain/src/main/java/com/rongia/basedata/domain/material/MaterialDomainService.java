package com.rongia.basedata.domain.material;

import com.rongia.basedata.domain.category.CategoryRepository;
import com.rongia.basedata.domain.unit.UnitRepository;
import com.rongia.basedata.domain.variant.Variant;
import com.rongia.basedata.domain.variant.VariantRepository;
import com.rongia.shared.domain.exception.DomainException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 物料领域服务
 *
 * <p>属性那套（Attribute 定义 / AttributeAssignment 赋值）已挪到平台层
 * {@code com.rongia.system.domain.attribute}；basedata-domain 不依赖 system-domain，物料聚合上的属性
 * 挂载暂时停用，本类里的属性校验与同步一并注释保留，接回来时恢复即可。公开的 {@code attributes} 入参
 * 先留着不生效，避免改签名牵连调用方。
 *
 * <p>校验只看属性定义，不要求代码同时是「参与规格生成的规格属性」（{@code SpecAttribute}）：两套定义各有
 * 用途，若在保存物料时强制查规格属性表，想在物料上记一个「重量」都得先去分类下建规格属性。
 */
@NullMarked
public class MaterialDomainService {

  /** 物料仓储 */
  private final MaterialRepository materialRepository;

  /** 分类仓储 */
  private final CategoryRepository categoryRepository;

  /** 规格仓储：物料代码变了要级联重算规格编码 */
  private final VariantRepository variantRepository;

  /** 单位仓储：采购单位必须和基本单位同组才能换算 */
  private final UnitRepository unitRepository;

  // 属性仓储随属性挂载停用
  // /** 属性仓储 */
  // private final AttributeRepository attributeRepository;

  /**
   * 构造器注入
   *
   * @param materialRepository 物料仓储
   * @param categoryRepository 分类仓储
   * @param variantRepository 规格仓储
   */
  public MaterialDomainService(
      MaterialRepository materialRepository,
      CategoryRepository categoryRepository,
      VariantRepository variantRepository,
      UnitRepository unitRepository) {
    this.materialRepository = materialRepository;
    this.categoryRepository = categoryRepository;
    this.variantRepository = variantRepository;
    this.unitRepository = unitRepository;
    // this.attributeRepository = attributeRepository;
  }

  // ==================== 公开：创建 ====================

  /**
   * 创建物料
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
   * @param attributes 属性值（可空，为 null 表示不改动）
   * @param attachmentIds 附件Id集合（可空）
   * @return 物料聚合
   * @throws DomainException 分类不存在 / 物料代码已存在 / 属性没有定义或取值不合法时抛出
   */
  public Material create(
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
      @Nullable Map<String, Object> attributes,
      @Nullable List<Long> attachmentIds) {
    // 1. 分类存在性
    validateCategory(categoryId);
    validateUnit(unitId);
    // 2. 代码唯一性
    if (materialRepository.isExistByCodeExcludeId(code)) {
      throw new DomainException("物料代码" + code + "已存在!");
    }
    // 3. 属性值按物料这一类型下的定义校验并摊平成行数据（随属性挂载停用）
    // List<AssignedAttributeData> attributeRows = resolveAttributes(attributes);
    // 4. 构造聚合并同步属性值行
    Material material =
        new Material(
            categoryId,
            type,
            code,
            name,
            description,            salesPrice,
            cost,            unitId,
            isPurchaseable,
            isSaleable,
            purchaseTaxRateId,
            salesTaxRateId,
            disabled,
            remark,
            attachmentIds);
    // material.syncAttributes(attributeRows);
    return material;
  }

  // ==================== 公开：导入 / 修改 ====================

  /**
   * 导入生成物料（存在则修改，不存在则新建）
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
   * @param attributes 属性值（可空，为 null 表示不改动）
   * @return 物料聚合
   * @throws DomainException 分类不存在 / 物料代码已存在 / 属性校验不通过时抛出
   */
  public Material importGenerate(
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
      @Nullable Map<String, Object> attributes) {
    // 存在则修改，不存在则新建
    Material model = materialRepository.getByCode(code);
    if (model == null) {
      model =
          create(
              categoryId,
              type,
              code,
              name,
              description,              salesPrice,
              cost,              unitId,
              isPurchaseable,
              isSaleable,
              purchaseTaxRateId,
              salesTaxRateId,
              disabled,
              remark,
              attributes,
              null);
    } else {
      modify(
          model,
          categoryId,
          type,
          code,
          name,
          description,          salesPrice,
          cost,          unitId,
          isPurchaseable,
          isSaleable,
          purchaseTaxRateId,
          salesTaxRateId,
          disabled,
          remark,
          attributes,
          null);
    }
    return model;
  }

  /**
   * 修改物料（按Id查询后修改）
   *
   * @param id 物料Id
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
   * @param attributes 属性值（可空，为 null 表示不改动）
   * @param attachmentIds 附件Id集合（可空）
   * @return 物料聚合
   * @throws DomainException 物料不存在 / 分类不存在 / 物料代码已存在时抛出
   */
  public Material modify(
      long id,
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
      @Nullable Map<String, Object> attributes,
      @Nullable List<Long> attachmentIds) {
    // 1. 查物料
    Material model = materialRepository.get(id);
    if (model == null) {
      throw new DomainException("物料不存在!");
    }
    // 2. 委托给使用已有聚合的重载
    modify(
        model,
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
        remark,
        attributes,
        attachmentIds);
    return model;
  }

  // ==================== 公开：修改（已有聚合） ====================

  /**
   * 修改物料（使用已有聚合，避免重复查询）
   *
   * @param material 物料聚合
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
   * @param attributes 属性值（可空，为 null 表示不改动）
   * @param attachmentIds 附件Id集合（可空）
   * @throws DomainException 分类不存在 / 物料代码已存在 / 属性校验不通过时抛出
   */
  public void modify(
      Material material,
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
      @Nullable Map<String, Object> attributes,
      @Nullable List<Long> attachmentIds) {
    // 1. 分类存在性
    validateCategory(categoryId);
    validateUnit(unitId);
    // 2. 代码唯一性（排除自身）
    if (materialRepository.isExistByCodeExcludeId(code, material.getId())) {
      throw new DomainException("物料代码" + code + "已存在!");
    }
    // 3. 属性值按物料这一类型下的定义校验（随属性挂载停用）
    // List<AssignedAttributeData> attributeRows = resolveAttributes(attributes);
    // 4. 记下旧代码：规格编码里带着物料代码，改了要跟着换
    String oldCode = material.getCode();

    // 5. 修改主字段
    material.modify(
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
        remark,
        attachmentIds);
    // 6. 物料代码变了，规格编码的前缀一起换（不换的话编码与物料对不上，唯一性也跟着错）
    if (!oldCode.equals(material.getCode())) {
      for (Variant variant : variantRepository.getListByMaterialId(material.getId())) {
        variant.recalcCode(material.getCode());
      }
    }
    // 7. 同步属性值行（按「代码 + 值」匹配，输入里没有的行标记删除）
    // material.syncAttributes(attributeRows);
  }

  // ==================== 私有：校验 ====================

  /**
   * 校验基本单位存在
   *
   * <p>采购单位不在物料上校验了：按什么单位进货随供应商变，那一列挪到了供应商物料卡，见
   * {@code com.rongia.basedata.domain.suppliermaterial.SupplierMaterial}。
   *
   * @param unitId 基本单位Id
   * @throws DomainException 单位不存在时抛出
   */
  private void validateUnit(long unitId) {
    if (unitRepository.get(unitId) == null) {
      throw new DomainException("单位" + unitId + "不存在!");
    }
  }

  /**
   * 校验分类存在
   *
   * @param categoryId 分类Id
   * @throws DomainException 分类不存在时抛出
   */
  private void validateCategory(long categoryId) {
    if (categoryRepository.get(categoryId) == null) {
      throw new DomainException("物料分类" + categoryId + "不存在!");
    }
  }

  // 属性值解析随属性挂载停用；接回来时按 Material 这一持有者类型取定义再解析
  // /**
  //  * 按物料这一持有者类型下的属性定义解析属性值
  //  *
  //  * <p>定义不按分类配：属性说的是「物料这类对象能挂什么描述」，跟物料归哪一类无关，否则同一个「重量」
  //  * 要在每个分类下重复配一遍。
  //  *
  //  * <p>入参为 null 时返回 null（聚合同步会跳过），传空 Map 才是清空，避免只改主字段的保存把已维护的
  //  * 属性值全删掉。
  //  *
  //  * @param attributes 属性值（可空）
  //  * @return 属性值行数据（可空）
  //  * @throws DomainException 属性没有定义、取值不合法或必填未给值时抛出
  //  */
  // @Nullable
  // private List<AssignedAttributeData> resolveAttributes(
  //     @Nullable Map<String, Object> attributes) {
  //   if (attributes == null) {
  //     return null;
  //   }
  //   return AttributeUtils.resolve(
  //       attributeRepository.getListByEntityType("Material"),
  //       attributes);
  // }
}
