package com.rongia.system.domain.attribute;

import com.rongia.shared.domain.entity.BaseDeleteEntity;
import com.rongia.shared.domain.exception.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Nationalized;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 属性值（属性的候选值，不是某个对象上填的值） */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "AttributeValue",
    indexes = {
      @Index(name = "IX_AttributeValue_AttributeId", columnList = "AttributeId"),
      @Index(name = "IX_AttributeValue_Value", columnList = "Value"),
      @Index(name = "IX_AttributeValue_Sequence", columnList = "Sequence")
      // 注意：AttributeId + Value 的过滤唯一索引带 WHERE IsDeleted=0，JPA 不支持，用 Flyway 创建
    })
public class AttributeValue extends BaseDeleteEntity {

  /** 属性Id */
  @Column(name = "AttributeId")
  private Long attributeId;

  /** 属性值（如：A、B、C） */
  @Nationalized
  @Column(name = "Value", nullable = false, length = 50)
  private String value;

  /** 属性值标签 */
  @Nationalized
  @Column(name = "Label", nullable = false, length = 200)
  private String label;

  /** 是否默认 */
  @Column(name = "IsDefault", nullable = false)
  private Boolean isDefault;

  /** 排序（可空） */
  @Column(name = "Sequence")
  @Nullable
  private Integer sequence;

  /**
   * 构造器
   *
   * @param value 属性值
   * @param label 属性值标签
   * @param isDefault 是否默认
   * @param sequence 排序（可空）
   */
  AttributeValue(String value, String label, boolean isDefault, @Nullable Integer sequence) {
    String trimmedValue = StringUtils.trimToNull(value);
    if (trimmedValue == null) {
      throw new DomainException("属性值不能为空");
    }
    // value 是属性值的业务主键，仅在创建时确定，之后不可变
    this.value = trimmedValue;
    apply(label, isDefault, sequence);
  }

  /**
   * 修改属性值
   *
   * @param label 属性值标签
   * @param isDefault 是否默认
   * @param sequence 排序（可空）
   */
  void modify(String label, boolean isDefault, @Nullable Integer sequence) {
    apply(label, isDefault, sequence);
  }

  // ==================== 私有：校验与赋值 ====================

  /**
   * 统一的可变字段校验与赋值入口（构造器与修改共用）
   *
   * <p>{@code value} 创建后不可变，因此这里只处理标签、默认标记与排序三个可变字段，避免两处各写一遍。
   *
   * @param label 属性值标签
   * @param isDefault 是否默认
   * @param sequence 排序（可空）
   */
  private void apply(String label, boolean isDefault, @Nullable Integer sequence) {
    String trimmedLabel = StringUtils.trimToNull(label);
    if (trimmedLabel == null) {
      throw new DomainException("属性值标签不能为空");
    }
    this.label = trimmedLabel;
    this.isDefault = isDefault;
    this.sequence = sequence;
  }
}
