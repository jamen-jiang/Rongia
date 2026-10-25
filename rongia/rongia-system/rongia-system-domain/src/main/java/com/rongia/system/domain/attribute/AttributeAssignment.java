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

/**
 * 属性赋值行（某个对象上填的一个「属性 + 值」，通用表）
 *
 * <p>所有对象填的属性值都落在这一张表：归属由 {@code EntityType}（持有者的表名，如 {@code Material} /
 * {@code Variant}）加 {@code EntityId} 两列一起确定，不再为每种持有者各建一张 {@code XxxAttribute}。
 * 这是 SAP {@code AUSP} 的做法（{@code OBJ_TYPE} + {@code OBJ_ID} + {@code ATTRIB} + {@code ATWRT}），
 * 好处是新对象要挂属性值不必再建表、不必再改聚合，取值规则只有一套。
 *
 * <p>{@code EntityType} 存表名字符串而不是枚举类：值就是数据库里那张表的名字，多一种持有者只是多一个
 * 字符串，不需要在共享枚举里再加一项。
 *
 * <p>{@code code} 是从定义冗余下来的一份（{@code AUSP} 也直接带 {@code ATTRN}）：读一个对象的属性值
 * 不必回定义表，拼显示串和排查问题时都能一眼看懂这一行是什么。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "AttributeAssignment",
    indexes = {
      @Index(
          name = "IX_AttributeAssignment_EntityType_EntityId",
          columnList = "EntityType, EntityId"),
      @Index(name = "IX_AttributeAssignment_AttributeId", columnList = "AttributeId"),
      @Index(name = "IX_AttributeAssignment_Code", columnList = "Code"),
      @Index(
          name = "IX_AttributeAssignment_EntityType_Code_Value",
          columnList = "EntityType, Code, Value")
      // 注意：EntityType + EntityId + AttributeId + Value 的过滤唯一索引带 WHERE IsDeleted=0，
      // JPA 不支持，用 Flyway 创建
    })
public class AttributeAssignment extends BaseDeleteEntity {

  /** 持有者表名（由持有者自己传，如 {@code Material} / {@code Variant}） */
  @Nationalized
  @Column(name = "EntityType", nullable = false, length = 50)
  private String entityType;

  /** 持有者Id（由持有者聚合的关联列写入，Java 侧不赋值） */
  @Column(name = "EntityId", nullable = false)
  private Long entityId;

  /** 属性定义Id */
  @Column(name = "AttributeId", nullable = false)
  private Long attributeId;

  /** 属性代码（冗余自定义） */
  @Nationalized
  @Column(name = "Code", nullable = false, length = 100)
  private String code;

  /** 属性值 */
  @Nationalized
  @Column(name = "Value", nullable = false, length = 200)
  private String value;

  /**
   * 构造器
   *
   * @param entityType 持有者表名（由持有者自己传，如 {@code Material} / {@code Variant}）
   * @param attributeId 属性定义Id
   * @param code 属性代码
   * @param value 属性值
   * @throws DomainException 持有者表名为空 / 代码为空 / 值为空时抛出
   */
  public AttributeAssignment(String entityType, long attributeId, String code, String value) {
    String trimmedEntityType = StringUtils.trimToNull(entityType);
    if (trimmedEntityType == null) {
      throw new DomainException("属性值的持有者不能为空");
    }
    String trimmedCode = StringUtils.trimToNull(code);
    if (trimmedCode == null) {
      throw new DomainException("属性代码不能为空");
    }
    String trimmedValue = StringUtils.trimToNull(value);
    if (trimmedValue == null) {
      throw new DomainException("属性值不能为空");
    }
    this.entityType = trimmedEntityType;
    this.attributeId = attributeId;
    this.code = trimmedCode;
    this.value = trimmedValue;
  }

  /**
   * 是否为「同一代码 + 同一值」的那一行
   *
   * @param code 属性代码
   * @param value 属性值
   * @return true：是同一行
   */
  public boolean isSame(String code, String value) {
    return this.code.equals(code) && this.value.equals(value);
  }
}
