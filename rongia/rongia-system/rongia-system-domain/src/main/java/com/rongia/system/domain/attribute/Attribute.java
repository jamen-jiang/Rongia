package com.rongia.system.domain.attribute;

import com.rongia.shared.domain.aggregate.UpdateAggregateRoot;
import com.rongia.shared.domain.enums.DataTypeEnum;
import com.rongia.shared.domain.enums.InputTypeEnum;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
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
 * 属性（属性的定义，不是某个对象上填的值）
 *
 * <p>与 {@code com.rongia.basedata.domain.specattribute.SpecAttribute} 同形
 * （代码、名称、数据类型、输入类型、候选值、校验规则），但用途不同：规格属性是「拿来做规格维度、 参与 SKU
 * 生成」的那一组定义，挂在分类下；属性是「挂在某类对象上的描述性取值」，挂在持有者类型上 （{@code EntityType} 存表名，如 {@code Material} / {@code
 * Variant}），不参与规格身份。理由：一个「重量」属性对成品和辅料写法一样，按分类配 就得每个分类重复配一遍；而哪些对象能挂属性是结构问题，跟物料归哪类无关。填到对象上的值另存通用赋值表
 * {@link AttributeAssignment}（同样是 {@code EntityType} + {@code EntityId}），相当于 SAP 把 {@code
 * CABN}（定义）与 {@code AUSP}（赋值，带 {@code OBJ_TYPE} + {@code OBJ_ID}）分在两张表。
 *
 * <p>候选值（{@link AttributeValue}）一旦维护了就变成硬约束：填的值必须是其中之一，否则同一个 「颜色」在十个物料上能写出十种写法；没有候选值时只按 {@code
 * DataType} 校验。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "Attribute",
    indexes = {
      @Index(name = "IX_Attribute_EntityType", columnList = "EntityType"),
      @Index(name = "IX_Attribute_Code", columnList = "Code"),
      @Index(name = "IX_Attribute_Label", columnList = "Label"),
      @Index(name = "IX_Attribute_Sequence", columnList = "Sequence")
      // 注意：EntityType + Code 的过滤唯一索引带 WHERE IsDeleted=0，JPA 不支持，用 Flyway 创建
    })
public class Attribute extends UpdateAggregateRoot {

  /** 持有者表名（由持有者自己传，如 {@code Material} / {@code Variant}，与 {@link AttributeAssignment} 同源） */
  @Nationalized
  @Column(name = "EntityType", nullable = false, length = 50)
  private String entityType;

  /** 属性代码（如：WEIGHT、GRADE） */
  @Nationalized
  @Column(name = "Code", nullable = false, length = 100)
  private String code;

  /** 属性标签 */
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

  /** 是否必填（该持有者类型下的对象必须给出值） */
  @Column(name = "IsRequired", nullable = false)
  private Boolean isRequired;

  /**
   * 是否作为筛选项（列表页可以按这个属性过滤，取值仍走 {@link AttributeAssignment}）
   *
   * <p>标在定义上而不是让每个页面自己记：ERPNext 的字段元数据带 {@code in_list_filter} / {@code
   * in_standard_filter}，金蝶云星空的自定义字段也自带「可过滤」设置，筛选项跟着定义走， 新增一个持有者不必再配一遍。
   */
  @Column(name = "IsFilter", nullable = false)
  private Boolean isFilter;

  /** 排序（可空） */
  @Column(name = "Sequence")
  @Nullable
  private Integer sequence;

  /** 备注（可空） */
  @Nationalized
  @Column(name = "Remark", length = 500)
  @Nullable
  private String remark;

  /** 属性值列表（只读） */
  @OneToMany(
      fetch = FetchType.EAGER,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE},
      orphanRemoval = true)
  @JoinColumn(name = "AttributeId")
  @SQLRestriction("IsDeleted = 0")
  private List<AttributeValue> attributeValues = new ArrayList<>();

  /**
   * 获取属性值列表（只读）
   *
   * @return 不可变列表
   */
  public List<AttributeValue> getAttributeValues() {
    return Collections.unmodifiableList(attributeValues);
  }

  /**
   * 构造器
   *
   * @param entityType 持有者表名（由持有者自己传，如 {@code Material} / {@code Variant}）
   * @param code 属性代码
   * @param label 属性标签
   * @param inputType 输入类型
   * @param dataType 数据类型
   * @param isRequired 是否必填
   * @param isFilter 是否作为筛选项
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   */
  Attribute(
      String entityType,
      String code,
      String label,
      String inputType,
      String dataType,
      boolean isRequired,
      boolean isFilter,
      @Nullable Integer sequence,
      @Nullable String remark) {
    String trimmedEntityType = StringUtils.trimToNull(entityType);
    if (trimmedEntityType == null) {
      throw new DomainException("属性持有者类型不能为空");
    }
    String trimmedCode = StringUtils.trimToNull(code);
    if (trimmedCode == null) {
      throw new DomainException("属性代码不能为空");
    }
    this.dataType = EnumUtils.getCode(DataTypeEnum.class, dataType, "数据类型");
    this.inputType = EnumUtils.getCode(InputTypeEnum.class, inputType, "输入类型");

    // 不可变字段：持有者类型、代码、数据类型创建后不允许修改，只在构造时赋值
    this.entityType = trimmedEntityType;
    this.code = trimmedCode;

    // 可变字段交给 apply 统一处理
    apply(label, inputType, isRequired, isFilter, sequence, remark);
  }

  /**
   * 修改属性
   *
   * <p>只能修改可变字段；持有者类型、代码、数据类型创建后不可变更。
   *
   * @param label 属性标签
   * @param inputType 输入类型
   * @param isRequired 是否必填
   * @param isFilter 是否作为筛选项
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   */
  public void modify(
      String label,
      String inputType,
      boolean isRequired,
      boolean isFilter,
      @Nullable Integer sequence,
      @Nullable String remark) {
    apply(label, inputType, isRequired, isFilter, sequence, remark);
  }

  /**
   * 新增属性值
   *
   * @param value 属性值
   * @param label 属性值标签
   * @param isDefault 是否默认项
   * @param sequence 排序（可空）
   * @throws DomainException 属性值在该属性下已存在时抛出
   */
  public void addValue(String value, String label, boolean isDefault, @Nullable Integer sequence) {
    if (isExistValue(value)) {
      throw new DomainException("属性" + this.label + ":属性值" + value + " 已存在!");
    }
    attributeValues.add(new AttributeValue(value, label, isDefault, sequence));
  }

  /**
   * 修改属性值
   *
   * <p>属性值（{@code value}）创建后不可修改，这里只能改标签、默认标记与排序。
   *
   * @param valueId 属性值Id
   * @param label 属性值标签
   * @param isDefault 是否默认项
   * @param sequence 排序（可空）
   * @throws DomainException 属性值不存在时抛出
   */
  public void modifyValue(
      long valueId, String label, boolean isDefault, @Nullable Integer sequence) {
    AttributeValue attributeValue = findValue(valueId);
    if (attributeValue == null) {
      throw new DomainException("属性" + this.label + ":属性值不存在!");
    }
    attributeValue.modify(label, isDefault, sequence);
  }

  /**
   * 删除属性值（逻辑删除）
   *
   * <p>找不到对应属性值时静默返回，保证删除操作幂等。
   *
   * @param valueId 属性值Id
   */
  public void deleteValue(long valueId) {
    AttributeValue attributeValue = findValue(valueId);
    if (attributeValue == null) {
      return;
    }
    attributeValue.delete();
  }

  /**
   * 校验一个取值能不能落到这个属性上
   *
   * <p>由领域服务解析出定义后逐个值调用：先按 {@code DataType} 校验写法，再在维护了候选值时要求
   * 值必须是候选值之一。表达式类的规则不在这里跑（和规格属性的校验规则一样，留给应用层执行）。
   *
   * @param value 取值
   * @throws DomainException 写法与数据类型不符、或不在候选值内时抛出
   */
  public void validateValue(String value) {
    DataTypeEnum type = EnumUtils.fromCode(DataTypeEnum.class, this.dataType);
    if (type == DataTypeEnum.INTEGER || type == DataTypeEnum.DECIMAL) {
      validateNumber(value, type);
    } else if (type == DataTypeEnum.BOOLEAN) {
      validateBoolean(value);
    } else if (type == DataTypeEnum.DATETIME) {
      validateDateTime(value);
    }

    List<String> candidates = candidateValues();
    if (!candidates.isEmpty() && !candidates.contains(StringUtils.trim(value))) {
      throw new DomainException("属性" + this.label + ":取值" + value + "不在属性值内!");
    }
  }

  /**
   * 必填校验：标了必填的属性必须拿到至少一个值
   *
   * <p>只在「这一笔调用确实在设置属性值」的路径上调用（创建、修改、导入）；只生成规格组合、 不碰属性值的路径不拦，否则生成 SKU 会被一堆没填的属性挡住。
   *
   * @param values 该属性拿到的值（可空）
   * @throws DomainException 必填却没给值时抛出
   */
  public void validateFilled(@Nullable List<String> values) {
    if (isRequired && (values == null || values.isEmpty())) {
      throw new DomainException("属性" + label + "必填!");
    }
  }

  /**
   * 候选值列表（未删除的属性值）
   *
   * @return 属性值代码列表
   */
  public List<String> candidateValues() {
    return attributeValues.stream()
        .filter(i -> !i.getIsDeleted())
        .map(AttributeValue::getValue)
        .toList();
  }

  // ==================== 私有：赋值 ====================

  /**
   * 统一的可变字段校验与赋值入口（构造器与修改共用）
   *
   * @param label 属性标签
   * @param inputType 输入类型
   * @param isRequired 是否必填
   * @param isFilter 是否作为筛选项
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   */
  private void apply(
      String label,
      String inputType,
      boolean isRequired,
      boolean isFilter,
      @Nullable Integer sequence,
      @Nullable String remark) {
    String trimmedLabel = StringUtils.trimToNull(label);
    if (trimmedLabel == null) {
      throw new DomainException("属性标签不能为空");
    }
    this.inputType = EnumUtils.getCode(InputTypeEnum.class, inputType, "输入类型");

    this.label = trimmedLabel;
    this.isRequired = isRequired;
    this.isFilter = isFilter;
    this.sequence = sequence;
    this.remark = remark;
  }

  // ==================== 私有：校验 ====================

  /**
   * 校验数值型取值
   *
   * @param value 取值
   * @param dataType 数据类型（整数、小数）
   * @throws DomainException 解析不出数字、整数位上有小数时抛出
   */
  private void validateNumber(String value, DataTypeEnum dataType) {
    BigDecimal number;
    try {
      number = new BigDecimal(StringUtils.trim(value));
    } catch (NumberFormatException e) {
      throw new DomainException("属性" + this.label + ":取值" + value + "不是数字!");
    }
    if (dataType == DataTypeEnum.INTEGER && number.stripTrailingZeros().scale() > 0) {
      throw new DomainException("属性" + this.label + ":取值" + value + "不是整数!");
    }
  }

  /**
   * 校验布尔型取值
   *
   * <p>只认 {@code true} / {@code false}：属性值列存的就是这个文本，域里不猜 {@code 1}/{@code yes}
   * 之类的写法，导入模板里的其他表示由导入层先归一再传进来。
   *
   * @param value 取值
   * @throws DomainException 值不是 true 或 false 时抛出
   */
  private void validateBoolean(String value) {
    String text = StringUtils.trimToEmpty(value);
    if (!"true".equalsIgnoreCase(text) && !"false".equalsIgnoreCase(text)) {
      throw new DomainException("属性" + this.label + ":取值" + value + "只能是布尔值!");
    }
  }

  /**
   * 校验日期型取值，接受 ISO 日期或日期时间两种写法
   *
   * @param value 取值
   * @throws DomainException 两种写法都解析不出时抛出
   */
  private void validateDateTime(String value) {
    String text = StringUtils.trimToEmpty(value);
    try {
      LocalDateTime.parse(text);
      return;
    } catch (DateTimeParseException ignored) {
      // 再按纯日期试一次
    }
    try {
      LocalDate.parse(text);
    } catch (DateTimeParseException e) {
      throw new DomainException("属性" + this.label + ":取值" + value + "不是日期!");
    }
  }

  // ==================== 私有：查找与存在性判断 ====================

  /**
   * 按Id查找属性值
   *
   * @param valueId 属性值Id
   * @return 属性值（可空），找不到返回 null
   */
  @Nullable
  private AttributeValue findValue(long valueId) {
    return attributeValues.stream()
        .filter(i -> Objects.equals(i.getId(), valueId))
        .findFirst()
        .orElse(null);
  }

  /**
   * 判断属性值是否已存在于当前属性的属性值列表中
   *
   * <p>已逻辑删除的属性值不参与判断，这样删除后可以重新添加相同属性值。
   *
   * @param value 属性值
   * @return true：已存在
   */
  private boolean isExistValue(String value) {
    return attributeValues.stream().anyMatch(i -> i.getValue().equals(value) && !i.getIsDeleted());
  }
}
