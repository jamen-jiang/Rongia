package com.rongia.basedata.domain.specattribute;

import com.rongia.shared.domain.aggregate.UpdateAggregateRoot;
import com.rongia.shared.domain.enums.DataTypeEnum;
import com.rongia.shared.domain.enums.InputTypeEnum;
import com.rongia.shared.domain.exception.DomainException;
import com.rongia.shared.domain.util.EnumUtils;
import jakarta.persistence.*;
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
 * 规格属性（维度的定义，不是某个规格上填的取值）
 *
 * <p>命名走 Odoo 那一套：参与变体展开的维度也是 attribute（{@code material.template.attribute.line}），
 * 展开出来的那一条是 variant（{@code Variant}）；国产 ERP 里对应的叫法是金蝶的「辅助属性」、用友的
 * 「自由项」，说的都是「同一物料编码下靠这组属性的取值组合来区分库存」这件事，中文统一写「规格属性」。
 * 与平台层的 {@code Attribute}（描述性字段定义）靠前缀区分：这张表属于物料主数据、挂在分类下、只用来展开
 * 规格；那张属于系统元数据、按 {@code EntityType} 挂到任何持有者上。不叫 {@code Characteristic}：SAP 用
 * 同一个词既指分类维度又指变体配置，跟 {@code Attribute} 更是直接近义撞词。
 *
 * <p>定义挂在分类下面（对标 SAP {@code CABN}）。填到对象上的值不放这张表——只有规格会填规格属性，取值一行
 * 一个落成 {@code VariantAttributeValue}（带 {@code VariantId} 外键），由这些行算出
 * {@code Variant.Code}（物料代码 + 属性组合）保证组合不重复；纯描述性的取值走属性那套（平台层的
 * {@code com.rongia.system.domain.attribute.Attribute} 定义 +
 * {@code com.rongia.system.domain.attribute.AttributeAssignment} 赋值行，相当于 SAP 的 {@code AUSP}）。
 *
 * <p>没有「是否关键规格属性」这个开关：分类下配的每一条规格属性都是给规格用的，一组值是不是构成规格身份
 * 由 SKU 自己决定（{@code VariantAttributeValue} 与属性赋值行分开存），不必在定义上再打一个标记——
 * 同一个代码既可以当规格维度也可以只作描述，硬编码成定义上的布尔值反而会互斥。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "SpecAttribute",
    indexes = {
      @Index(name = "IX_SpecAttribute_CategoryId", columnList = "CategoryId"),
      @Index(name = "IX_SpecAttribute_Code", columnList = "Code"),
      @Index(name = "IX_SpecAttribute_Label", columnList = "Label"),
      @Index(name = "IX_SpecAttribute_Sequence", columnList = "Sequence")
      // 注意：CategoryId + Code 的过滤唯一索引带 WHERE IsDeleted=0，JPA 不支持，用 Flyway 创建
    })
public class SpecAttribute extends UpdateAggregateRoot {

  /** 所属分类Id */
  @Column(name = "CategoryId", nullable = false)
  private Long categoryId;

  /** 规格属性代码（如：SIZE、MATERIAL） */
  @Nationalized
  @Column(name = "Code", nullable = false, length = 100)
  private String code;

  /** 规格属性标签 */
  @Nationalized
  @Column(name = "Label", nullable = false, length = 200)
  private String label;

  /** 输入类型编码，取值见 {@link InputTypeEnum} */
  @Nationalized
  @Column(name = "InputType", nullable = false, length = 50)
  private String inputType;

  /** 数据类型编码，取值见 {@link DataTypeEnum} */
  @Nationalized
  @Column(name = "DataType", nullable = false, length = 50)
  private String dataType;

/** 排序（可空） */
  @Column(name = "Sequence")
  @Nullable
  private Integer sequence;

  /** 备注（可空） */
  @Nationalized
  @Column(name = "Remark", length = 500)
  @Nullable
  private String remark;

  /** 规格属性值列表（只读） */
  @OneToMany(
      fetch = FetchType.EAGER,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE},
      orphanRemoval = true)
  @JoinColumn(name = "SpecAttributeId")
  @SQLRestriction("IsDeleted = 0")
  private List<SpecAttributeValue> specAttributeValues = new ArrayList<>();

  /**
   * 获取规格属性值列表（只读）
   *
   * @return 不可变列表
   */
  public List<SpecAttributeValue> getSpecAttributeValues() {
    return Collections.unmodifiableList(specAttributeValues);
  }

  /**
   * 构造器
   *
   * @param categoryId 所属分类Id
   * @param code 规格属性代码
   * @param label 规格属性标签
   * @param inputType 输入类型
   * @param dataType 数据类型
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   */
  SpecAttribute(
      long categoryId,
      String code,
      String label,
      String inputType,
      String dataType,
      @Nullable Integer sequence,
      @Nullable String remark) {
    String trimmedCode = StringUtils.trimToNull(code);
    if (trimmedCode == null) {
      throw new DomainException("规格属性代码不能为空");
    }
    this.dataType = EnumUtils.getCode(DataTypeEnum.class, dataType, "数据类型");

    // 不可变字段：所属分类、代码、数据类型创建后不允许修改，只在构造时赋值
    this.categoryId = categoryId;
    this.code = trimmedCode;

    // 可变字段交给 apply 统一处理
    apply(label, inputType, sequence, remark);
  }

  /**
   * 修改规格属性
   *
   * <p>只能修改可变字段；所属分类、代码、数据类型创建后不可变更。
   *
   * @param label 规格属性标签
   * @param inputType 输入类型
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   */
  public void modify(
      String label,
      String inputType,
      @Nullable Integer sequence,
      @Nullable String remark) {
    apply(label, inputType, sequence, remark);
  }

  /**
   * 新增规格属性值
   *
   * @param value 规格属性值
   * @param label 规格属性值标签
   * @param isDefault 是否默认项
   * @param sequence 排序（可空）
   * @throws DomainException 规格属性值在该规格属性下已存在时抛出
   */
  public void addValue(String value, String label, boolean isDefault, @Nullable Integer sequence) {
    if (isExistValue(value)) {
      throw new DomainException("规格属性" + this.label + ":规格属性值" + value + " 已存在!");
    }
    SpecAttributeValue specAttributeValue =
        new SpecAttributeValue(value, label, isDefault, sequence);
    specAttributeValues.add(specAttributeValue);
  }

  /**
   * 修改规格属性值
   *
   * <p>规格属性值（{@code value}）创建后不可修改，这里只能改标签、默认标记与排序。
   *
   * @param valueId 规格属性值Id
   * @param label 规格属性值标签
   * @param isDefault 是否默认项
   * @param sequence 排序（可空）
   * @throws DomainException 规格属性值不存在时抛出
   */
  public void modifyValue(long valueId, String label, boolean isDefault, @Nullable Integer sequence) {
    SpecAttributeValue specAttributeValue = findValue(valueId);
    if (specAttributeValue == null) {
      throw new DomainException("规格属性" + this.label + ":规格属性值不存在!");
    }
    specAttributeValue.modify(label, isDefault, sequence);
  }

  /**
   * 删除规格属性值（逻辑删除）
   *
   * <p>找不到对应规格属性值时静默返回，保证删除操作幂等。
   *
   * @param valueId 规格属性值Id
   */
  public void deleteValue(long valueId) {
    SpecAttributeValue specAttributeValue = findValue(valueId);
    if (specAttributeValue == null) {
      return;
    }
    specAttributeValue.delete();
  }

  // ==================== 私有：校验与赋值 ====================

  /**
   * 统一的可变字段校验与赋值入口（构造器与修改共用）
   *
   * <p>只处理允许修改的字段；所属分类、代码、数据类型属于不可变字段，由调用方单独赋值。
   *
   * @param label 规格属性标签
   * @param inputType 输入类型
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   */
  private void apply(
      String label,
      String inputType,
      @Nullable Integer sequence,
      @Nullable String remark) {
    String trimmedLabel = StringUtils.trimToNull(label);
    if (trimmedLabel == null) {
      throw new DomainException("规格属性标签不能为空");
    }
    this.inputType = EnumUtils.getCode(InputTypeEnum.class, inputType, "输入类型");
    this.label = trimmedLabel;
    this.sequence = sequence;
    this.remark = remark;
  }

  // ==================== 私有：查找与存在性判断 ====================

  /**
   * 按Id查找规格属性值
   *
   * @param valueId 规格属性值Id
   * @return 规格属性值（可空），找不到返回 null
   */
  @Nullable
  private SpecAttributeValue findValue(long valueId) {
    return specAttributeValues.stream()
        .filter(i -> Objects.equals(i.getId(), valueId))
        .findFirst()
        .orElse(null);
  }

  /**
   * 判断规格属性值是否已存在于当前规格属性的规格属性值列表中
   *
   * <p>已逻辑删除的规格属性值不参与判断，这样删除后可以重新添加相同规格属性值。
   *
   * @param value 规格属性值
   * @return true：已存在
   */
  private boolean isExistValue(String value) {
    return specAttributeValues.stream()
        .anyMatch(i -> i.getValue().equals(value) && !i.getIsDeleted());
  }
}
