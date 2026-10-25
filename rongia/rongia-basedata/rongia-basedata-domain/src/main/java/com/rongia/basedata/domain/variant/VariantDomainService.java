package com.rongia.basedata.domain.variant;

import com.rongia.basedata.domain.material.Material;
import com.rongia.basedata.domain.material.MaterialRepository;
import com.rongia.basedata.domain.specattribute.AssignedSpecAttributeData;
import com.rongia.basedata.domain.specattribute.SpecAttribute;
import com.rongia.basedata.domain.specattribute.SpecAttributeRepository;
import com.rongia.shared.domain.exception.DomainException;
import com.rongia.shared.domain.util.ValueTextUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * SKU 领域服务
 *
 * <p>规格属性的归属用「分类」表达：SKU 自己不存物料类型，需要校验规格属性时顺着 {@code materialId} 找到物料， 再取物料的 {@code
 * categoryId}。这样不会出现「SKU 说是成品、物料却是辅料」这种两张皮的数据。
 *
 * <p>分类下配的规格属性就是规格的维度：SKU 不从属性值自动批量生成（照 ERPNext——属性值维护完什么都不会发生，
 * 建哪些组合由人显式发起），单据选到具体组合时由 {@code getOrCreate} 现建，没被选过的组合永远不占 SKU 行。
 *
 * <p>描述性取值本来是另一套（平台层的 {@code Attribute} 定义 + {@code AttributeAssignment} 赋值行），
 * 这套已挪出本模块，规格聚合上的属性挂载随之停用：公开方法的 {@code attributes} 入参先留着但不生效， 恢复挂载时把注释里的 {@code resolveAttributes}
 * 与 {@code syncAttributes} 接回来即可。
 */
@NullMarked
public class VariantDomainService {

  /** SKU 仓储 */
  private final VariantRepository variantRepository;

  /** 物料仓储 */
  private final MaterialRepository materialRepository;

  /** 规格属性仓储 */
  private final SpecAttributeRepository specAttributeRepository;

  // 属性仓储：属性那套定义/赋值已挪到平台层 com.rongia.system.domain.attribute，本模块不依赖
  // system-domain，挂载先停用，接回来时再放开下面两处。
  // /** 属性仓储 */
  // private final AttributeRepository attributeRepository;

  /**
   * 构造器注入
   *
   * @param variantRepository SKU 仓储
   * @param materialRepository 物料仓储
   * @param specAttributeRepository 规格属性仓储
   */
  public VariantDomainService(
      VariantRepository variantRepository,
      MaterialRepository materialRepository,
      SpecAttributeRepository specAttributeRepository) {
    this.variantRepository = variantRepository;
    this.materialRepository = materialRepository;
    this.specAttributeRepository = specAttributeRepository;
    // this.attributeRepository = attributeRepository;
  }

  // ==================== 公开：创建 / 修改 ====================

  /**
   * 创建 SKU（入参为 Id，不解析 Code）
   *
   * @param materialId MaterialId
   * @param barcode 条码（可空）
   * @param specAttributes 规格属性值——这一条规格由哪组值构成（可空）
   * @param attributes 属性值（可空，属性挂载已停用，暂不生效）
   * @return SKU 聚合
   * @throws DomainException 物料不存在 / 规格属性不存在 / 规格属性组合重复时抛出
   */
  public Variant create(
      long materialId,
      @Nullable String barcode,
      @Nullable Map<String, Object> specAttributes,
      @Nullable Map<String, Object> attributes) {
    Material material = getExistMaterial(materialId);
    return buildVariant(
        materialId,
        material.getCode(),
        barcode,
        resolveSpecAttributes(material.getCategoryId(), specAttributes));
  }

  /**
   * 导入生成（存在则修改，不存在则新建）
   *
   * @param materialId 物料Id
   * @param barcode 条码（可空）
   * @param specAttributes 规格属性值（可空）
   * @param attributes 属性值（可空，属性挂载已停用，暂不生效）
   * @return SKU 聚合
   * @throws DomainException 物料不存在 / 规格属性不存在时抛出
   */
  public Variant importGenerate(
      long materialId,
      @Nullable String barcode,
      @Nullable Map<String, Object> specAttributes,
      @Nullable Map<String, Object> attributes) {
    Material material = getExistMaterial(materialId);
    // 1. 规格属性值落成行数据（代码必须在物料所属分类下有定义）
    List<AssignedSpecAttributeData> specAttributeRows =
        resolveSpecAttributes(material.getCategoryId(), specAttributes);

    // 2. 按规格编码查询，存在则修改，不存在则新建
    String code = Variant.buildCode(material.getCode(), specAttributeRows);
    Variant model = variantRepository.getByCode(code);

    if (model == null) {
      model = buildVariant(materialId, material.getCode(), barcode, specAttributeRows);
    } else {
      modify(model, barcode);
    }
    return model;
  }

  /**
   * 修改（按Id查询后修改）
   *
   * <p>规格属性值不可改：那组行就是这条规格的身份，换一组等于换一条 SKU，应该走删除 + 重新生成。
   *
   * @param id SKU Id
   * @param barcode 条码（可空）
   * @param attributes 属性值（可空，属性挂载已停用，暂不生效）
   * @return SKU 聚合
   * @throws DomainException SKU 不存在 / 物料不存在时抛出
   */
  public Variant modify(
      long id,
      @Nullable String barcode,
      @Nullable Map<String, Object> attributes) {
    Variant model = variantRepository.get(id);
    if (model == null) {
      throw new DomainException("物料Variant不存在!");
    }
    modify(model, barcode);
    return model;
  }

  /**
   * 按一组规格取值取 SKU：有就返回，没有就建一条
   *
   * <p>变体不自动生成：属性值维护好之后什么都不会发生，等单据选到具体组合时才在这里现建一条，
   * 没被选过的组合永远不占 SKU 行（照 ERPNext 的 Single Variant / Multiple Variants——都是人显式发起）。
   *
   * @param materialId MaterialId
   * @param specAttributes 规格属性值（可空，可空表示没有规格维度的物料）
   * @return 需要持久化的聚合：已存在时原样返回，不存在时返回新建的那条
   * @throws DomainException 物料不存在、或规格属性不存在时抛出
   */
  public Variant getOrCreate(
      long materialId, @Nullable Map<String, Object> specAttributes) {
    Material material = getExistMaterial(materialId);
    List<AssignedSpecAttributeData> rows =
        resolveSpecAttributes(material.getCategoryId(), specAttributes);
    String code = Variant.buildCode(material.getCode(), rows);
    Variant existing = variantRepository.getByCode(code);
    if (existing != null) {
      return existing;
    }
    return buildVariant(material.getId(), material.getCode(), null, rows);
  }

  // ==================== 私有：创建 / 修改 ====================

  /**
   * 构建 SKU（含规格属性组合唯一性校验）
   *
   * @param materialId MaterialId
   * @param barcode 条码（可空）
   * @param specAttributeRows 规格属性值行数据（已校验）
   * @return SKU
   * @throws DomainException 规格属性组合重复时抛出
   */
  private Variant buildVariant(
      long materialId,
      String materialCode,
      @Nullable String barcode,
      List<AssignedSpecAttributeData> specAttributeRows) {
    // 1. 构造规格（规格属性值行在构造时写入）
    Variant model = new Variant(materialId, materialCode, barcode, specAttributeRows);

    // 2. 规格编码唯一性
    if (variantRepository.isExistByCode(model.getCode())) {
      throw new DomainException("相同规格属性组合的规格已存在!");
    }

    return model;
  }

  /**
   * 修改（使用已有聚合，避免重复查询）
   *
   * @param model SKU 聚合
   * @param barcode 条码（可空）
   */
  private void modify(Variant model, @Nullable String barcode) {
    // 修改（只动可变字段，规格属性值行保持不变）
    model.modify(barcode);
  }

  // ==================== 私有：规格属性值解析 ====================

  /**
   * 解析规格属性值入参：按代码在物料所属分类下找定义，摊平成「一个取值一行」的行数据
   *
   * <p>代码对不上就当场报错：编码是按属性代码拼的，留下一个分类下没有的代码，重新生成时这条 SKU 会被当成「取值已失效」删掉。
   *
   * <p>取值存维护好的原样文本，不做按 {@code DataType} 的类型换算——数字规范只在拼编码时做， 否则 {@code 0755} 这种当编码用的取值会被削成 {@code
   * 755}。
   *
   * @param categoryId 物料分类Id
   * @param specAttributes 规格属性值（可空，一个值可以是标量或数组）
   * @return 规格属性值行数据，没有给值时返回空列表
   * @throws DomainException 规格属性不存在时抛出
   */
  private List<AssignedSpecAttributeData> resolveSpecAttributes(
      long categoryId, @Nullable Map<String, Object> specAttributes) {
    Map<String, List<String>> flattened = ValueTextUtils.flatten(specAttributes);
    if (flattened.isEmpty()) {
      return List.of();
    }
    Map<String, SpecAttribute> dict = new TreeMap<>();
    for (SpecAttribute definition : specAttributeRepository.getListByCategoryId(categoryId)) {
      dict.put(definition.getCode(), definition);
    }

    List<AssignedSpecAttributeData> rows = new ArrayList<>();
    for (Map.Entry<String, List<String>> entry : flattened.entrySet()) {
      SpecAttribute definition = dict.get(entry.getKey());
      if (definition == null) {
        throw new DomainException("规格属性" + entry.getKey() + "不存在!");
      }
      for (String value : entry.getValue()) {
        ensureValueAllowed(definition, value);
        rows.add(new AssignedSpecAttributeData(definition.getId(), definition.getCode(), value));
      }
    }
    return rows;
  }

  /**
   * 校验取值在规格属性维护好的值里
   *
   * <p>与 Odoo 一致：规格属性上没维护的取值不能拿来建 SKU，先报错让人回去补值，而不是当场造一条谁也没配过的
   * 度数。按需生成省掉的是「预建 SKU」，不是省掉「值要先维护」——值就是客户能选的范围，没维护的值等于不存在。
   *
   * <p>比较走规范化文本，{@code 25}、{@code 25.00}、{@code "25"} 算同一个值。
   *
   * @param definition 规格属性定义
   * @param value 待校验的取值
   * @throws DomainException 取值不在该规格属性维护的值里时抛出
   */
  private void ensureValueAllowed(SpecAttribute definition, String value) {
    String canonical = ValueTextUtils.canonicalText(value);
    boolean allowed =
        definition.getSpecAttributeValues().stream()
            .filter(v -> !v.getIsDeleted())
            .anyMatch(v -> ValueTextUtils.canonicalText(v.getValue()).equals(canonical));
    if (!allowed) {
      throw new DomainException(
          "规格属性" + definition.getCode() + "的取值" + value + "没有维护，请先在规格属性值里加上!");
    }
  }

  // ==================== 私有：取数 ====================

  /**
   * 取物料，不存在则抛异常
   *
   * @param materialId MaterialId
   * @return 物料聚合
   * @throws DomainException 物料不存在时抛出
   */
  private Material getExistMaterial(long materialId) {
    Material material = materialRepository.get(materialId);
    if (material == null) {
      throw new DomainException("物料" + materialId + "不存在!");
    }
    return material;
  }

  // ==================== 私有：属性值解析（已停用） ====================

  // /**
  //  * 按规格这一持有者类型下的属性定义解析属性值
  //  *
  //  * <p>定义不按分类配：属性说的是「规格这类对象能挂什么描述」，跟物料归哪一类无关。
  //  *
  //  * <p>入参为 null 时返回 null，聚合同步会跳过这一步——只带规格组合的导入不能顺手把已维护的属性值清空。
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
  //       attributeRepository.getListByEntityType("Variant"),
  //       attributes);
  // }
}
