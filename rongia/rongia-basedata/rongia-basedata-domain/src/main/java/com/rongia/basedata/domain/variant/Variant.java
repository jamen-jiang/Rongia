package com.rongia.basedata.domain.variant;

import com.rongia.basedata.domain.specattribute.AssignedSpecAttributeData;
import com.rongia.shared.domain.aggregate.UpdateAggregateRoot;
import com.rongia.shared.domain.exception.DomainException;
import com.rongia.shared.domain.util.ValueTextUtils;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
 * Variant（一条规格型号，业务口中就是 SKU）
 *
 * <p>这里只放「这个规格是什么」：条码、规格属性值。采购价、币种、起订量不在这里，见 {@code
 * com.rongia.basedata.domain.suppliermaterial.SupplierMaterial}——同一个规格可以被多个供应商报价， 钉在规格上只能留下最新一个值。
 *
 * <p>值分两处存，是因为它们的作用根本不同：{@link VariantSpecAttributeValue} 是这一行的身份 （哪一组值构成一个规格），一行一个取值，改它就等于换一条规格，所以
 * {@code modify} 不碰它， 唯一性由这些行拼成 {@code Code}（物料代码 + 属性组合）来保证；描述性取值另存通用赋值表 {@code
 * AttributeAssignment}（{@code EntityType = 'Variant'}，定义在 {@code Attribute}，相当于 SAP 的 {@code CABN}
 * 与 {@code AUSP}），改它不影响身份。 Odoo 也是这个分法：变体组合由 {@code material.template.attribute.line} 展开成 {@code
 * material.material} 的一行，其余描述字段不进入组合唯一性（{@code product_product_combination_unique}）。
 *
 * <p>那张赋值表已挪到平台层 {@code com.rongia.system.domain.attribute}，本模块不依赖 system-domain，
 * 所以规格聚合上的属性挂载暂时停用，见字段处注释。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "Variant",
    indexes = {
      @Index(name = "IX_Variant_MaterialId", columnList = "MaterialId"),
      @Index(name = "IX_Variant_Code", columnList = "Code")
      // 注意：Code 的过滤唯一索引带 WHERE IsDeleted=0，用 Flyway 创建
    })
public class Variant extends UpdateAggregateRoot {

  /** 所属物料Id */
  @Column(name = "MaterialId", nullable = false)
  private Long materialId;

  /** 规格编码：物料代码 + 规格属性组合，人可读，唯一性靠它 */
  @Nationalized
  @Column(name = "Code", nullable = false, length = 500)
  private String code;

  /** 条码（可空）：每个规格可不同，照 Odoo 的 barcode */
  @Nationalized
  @Column(name = "Barcode", length = 100)
  @Nullable
  private String barcode;

  /** 是否禁用 */
  @Column(name = "Disabled", nullable = false)
  private Boolean disabled = false;

  /**
   * 规格属性值行（构成规格身份，参与唯一性判断，只读）
   *
   * <p>一个取值一行，所以同一个代码可以有多行；这一组行合起来才是「这个规格是什么」。
   */
  @OneToMany(
      fetch = FetchType.EAGER,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE},
      orphanRemoval = true)
  @JoinColumn(name = "VariantId")
  @SQLRestriction("IsDeleted = 0")
  private List<VariantSpecAttributeValue> specAttributes = new ArrayList<>();

  /**
   * 获取规格属性值行（只读）
   *
   * @return 不可变列表
   */
  public List<VariantSpecAttributeValue> getSpecAttributes() {
    return Collections.unmodifiableList(specAttributes);
  }

  /**
   * 获取规格属性值（代码 → 值）
   *
   * <p>一个代码只有一行时给出标量，多行时给出列表，读的人不必先数行数。
   *
   * @return 按代码排序的规格属性值
   */
  public TreeMap<String, Object> specAttributeValues() {
    TreeMap<String, Object> result = new TreeMap<>();
    for (Map.Entry<String, List<String>> entry : groupValues().entrySet()) {
      List<String> values = entry.getValue();
      result.put(entry.getKey(), values.size() == 1 ? values.getFirst() : List.copyOf(values));
    }
    return result;
  }

  /**
   * 构造器
   *
   * @param materialId MaterialId
   * @param materialCode 物料代码（拼进规格编码，改名时由物料那边级联重算）
   * @param barcode 条码（可空）
   * @param specAttributeRows 规格属性值行数据——这一条规格由哪组值构成（可空）
   */
  Variant(
      long materialId,
      String materialCode,
      @Nullable String barcode,
      @Nullable List<AssignedSpecAttributeData> specAttributeRows) {
    // 不可变字段：MaterialId 与规格属性值行（决定唯一性编码）创建后不允许修改
    this.materialId = materialId;
    if (specAttributeRows != null) {
      for (AssignedSpecAttributeData data : specAttributeRows) {
        this.specAttributes.add(
            new VariantSpecAttributeValue(data.specAttributeId(), data.code(), data.value()));
      }
    }

    // 可变字段交给 apply 统一处理
    apply(barcode);

    // 编码依赖物料代码 + 规格属性值行，必须在两者赋值完成后计算
    this.code = buildCode(materialCode, groupValues());
  }

  /**
   * 修改
   *
   * @param barcode 条码（可空）
   */
  void modify(@Nullable String barcode) {
    apply(barcode);
  }

  /**
   * 按新的物料代码重算规格编码（物料改名时由物料那边级联调用）
   *
   * <p>规格属性值定死后不会变，所以重算只换前缀；唯一性由编码那一条过滤唯一索引兜。
   *
   * @param materialCode 新的物料代码
   * @throws DomainException 物料代码为空，或拼出的编码超过列长时抛出
   */
  public void recalcCode(String materialCode) {
    this.code = buildCode(materialCode, groupValues());
  }

  /** 启用 */
  public void enable() {
    this.disabled = false;
  }

  /** 禁用 */
  public void disable() {
    this.disabled = true;
  }

  /** 删除 */
  @Override
  public void delete() {
    super.delete();
    addDomainEvent(new VariantDeletedEvent(this));
  }

  // ---------- 规格编码 ----------

  /**
   * 静态工具方法：在不创建实例的情况下拼出规格编码，用于按组合查已有规格
   *
   * <p>参与拼接的是「规格属性代码=规格属性值」而不是只有值，否则 {@code {SIZE:25}} 与 {@code {COLOR:25}} 会拼成同一个编码。
   *
   * @param materialCode 物料代码
   * @param specAttributeRows 规格属性值行数据（可空）
   * @return 规格编码，形如 {@code MG0050|COLOR=蓝|SIZE=50}
   */
  public static String buildCode(
      String materialCode, @Nullable List<AssignedSpecAttributeData> specAttributeRows) {
    Map<String, List<String>> grouped = new TreeMap<>();
    if (specAttributeRows != null) {
      for (AssignedSpecAttributeData data : specAttributeRows) {
        grouped.computeIfAbsent(data.code(), code -> new ArrayList<>()).add(data.value());
      }
    }
    return buildCode(materialCode, grouped);
  }

  /**
   * 当前有效规格属性值行按代码归并
   *
   * @return 代码 → 值列表，按代码排序
   */
  private Map<String, List<String>> groupValues() {
    Map<String, List<String>> grouped = new TreeMap<>();
    for (VariantSpecAttributeValue row : specAttributes) {
      if (row.getIsDeleted()) {
        continue;
      }
      grouped.computeIfAbsent(row.getCode(), code -> new ArrayList<>()).add(row.getValue());
    }
    return grouped;
  }

  /**
   * 把「物料代码 + 代码 → 值列表」拼成规格编码
   *
   * <p>用 {@code |} 和 {@code =} 当分隔符：属性值里常见 {@code -} 和 {@code :}（比如 {@code Φ50-蓝膜}），
   * 拿它们拼会拼出有歧义的编码。拼之前两样东西都不许出现——{@code |}、{@code =} 出现在物料代码或规格属性
   * 代码/值里直接报错，不做转义：转义一多，编码就不再"人可读"，这个改动就白做了。
   *
   * <p>同一代码的多个值先规范化再排序（{@code 25}、{@code 25.00} 都成 {@code 25}），否则同一个组合换个 录入顺序就成了另一条规格。
   *
   * @param materialCode 物料代码
   * @param grouped 代码 → 值列表（已按代码排序）
   * @return 规格编码，超长直接报错、不截断（截断会让两个长值撞成同一个编码）
   * @throws DomainException 代码为空、含分隔符，或拼出来超过 500 个字符时抛出
   */
  private static String buildCode(String materialCode, Map<String, List<String>> grouped) {
    String trimmedMaterialCode = StringUtils.trimToNull(materialCode);
    if (trimmedMaterialCode == null) {
      throw new DomainException("物料代码不能为空，规格编码要靠它拼");
    }
    validateCodePart("物料代码", trimmedMaterialCode);

    StringBuilder builder = new StringBuilder(trimmedMaterialCode);
    for (Map.Entry<String, List<String>> entry : grouped.entrySet()) {
      String code = entry.getKey();
      String text = valuesText(entry.getValue());
      validateCodePart("规格属性代码", code);
      validateCodePart("规格属性值", text);
      builder.append('|').append(code).append('=').append(text);
    }

    String result = builder.toString();
    if (result.length() > 500) {
      throw new DomainException("规格编码超过500个字符，请缩短物料代码或规格属性值：" + result);
    }
    return result;
  }

  /**
   * 校验编码片段里没有分隔符
   *
   * @param field 字段中文名，用于报错
   * @param text 待校验片段
   * @throws DomainException 出现 {@code |} 或 {@code =} 时抛出
   */
  private static void validateCodePart(String field, String text) {
    if (text.contains("|") || text.contains("=")) {
      throw new DomainException(field + "里不能出现 | 或 =：" + text);
    }
  }

  /**
   * 把同一代码的多个规格属性值规范成编码文本
   *
   * <p>{@code 25}、{@code 25.00}、{@code "25"} 都规范成 {@code "25"}，手填的和区间展开的才能算出 同一条规格。
   *
   * @param values 同一代码下的规格属性值
   * @return 排序后用逗号拼接的文本
   */
  private static String valuesText(List<String> values) {
    List<String> texts = values.stream().map(ValueTextUtils::canonicalText).sorted().toList();
    return String.join(",", texts);
  }

  // ---------- 变更字段 ----------

  /**
   * 统一的可变字段赋值入口（构造器与修改共用）
   *
   * <p>{@code materialId}、规格属性值行属于不可变字段（决定唯一性编码），由调用方单独赋值；其余字段全部在此 收敛，避免构造器与 {@link #modify}
   * 两处各写一遍导致漏改。
   *
   * @param barcode 条码（可空）
   */
  private void apply(@Nullable String barcode) {
    this.barcode = StringUtils.trimToNull(barcode);
  }
}
