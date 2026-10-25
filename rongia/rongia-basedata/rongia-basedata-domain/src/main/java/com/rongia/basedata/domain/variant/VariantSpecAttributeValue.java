package com.rongia.basedata.domain.variant;

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
 * 规格属性值行（这一条 SKU 由哪个规格属性的哪个取值构成，一行一个值）
 *
 * <p>按持有者命名、用 {@code VariantId} 直接外键，而不是挤进通用赋值表：规格属性值只有规格会填
 * （物料的描述性取值走属性那套），没有第二种持有者，通用表换不来任何东西，只会多出一个 「靠 {@code EntityType} + {@code EntityId} 猜归属」的步骤。SAP 的
 * {@code AUSP} 之所以通用， 是因为同一套分类要挂物料、单据、设备好几种对象，这里没这个需求。
 *
 * <p>这些行就是这一条规格的身份：改一行等于换一条规格，所以只在建 SKU 时写入，{@code modify} 不碰它们；{@code Variant.Code}
 * 由这些行拼出来，重复组合靠编码上的过滤唯一索引拦。
 *
 * <p>{@code code} 从定义冗余一份，读一条规格是什么不必回定义表；{@code value} 存维护好的原样文本， 只在拼编码时做数字规范化，否则 {@code 25} 与
 * {@code 25.00} 会拼成两条规格。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "VariantAttributeValue",
    indexes = {
      @Index(name = "IX_VariantAttributeValue_VariantId", columnList = "VariantId"),
      @Index(name = "IX_VariantAttributeValue_Code", columnList = "Code")
      // 注意：VariantId + SpecAttributeId + Value 的过滤唯一索引带 WHERE IsDeleted=0，
      // JPA 不支持，用 Flyway 创建
    })
public class VariantSpecAttributeValue extends BaseDeleteEntity {

  /** 所属SKU Id（由 SKU 聚合的关联列写入，Java 侧不赋值） */
  @Column(name = "VariantId")
  private Long variantId;

  /** 规格属性定义Id */
  @Column(name = "SpecAttributeId", nullable = false)
  private Long specAttributeId;

  /** 规格属性代码（冗余自定义） */
  @Nationalized
  @Column(name = "Code", nullable = false, length = 100)
  private String code;

  /** 规格属性值 */
  @Nationalized
  @Column(name = "Value", nullable = false, length = 200)
  private String value;

  /**
   * 构造器
   *
   * @param specAttributeId 规格属性定义Id
   * @param code 规格属性代码
   * @param value 规格属性值
   * @throws DomainException 代码为空 / 值为空时抛出
   */
  public VariantSpecAttributeValue(long specAttributeId, String code, String value) {
    String trimmedCode = StringUtils.trimToNull(code);
    if (trimmedCode == null) {
      throw new DomainException("规格属性代码不能为空");
    }
    String trimmedValue = StringUtils.trimToNull(value);
    if (trimmedValue == null) {
      throw new DomainException("规格属性值不能为空");
    }
    this.specAttributeId = specAttributeId;
    this.code = trimmedCode;
    this.value = trimmedValue;
  }
}
