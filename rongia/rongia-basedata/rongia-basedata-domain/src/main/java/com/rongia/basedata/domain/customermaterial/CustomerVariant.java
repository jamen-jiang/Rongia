package com.rongia.basedata.domain.customermaterial;

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

/**
 * 客户SKU（个别SKU对方另有编号）
 *
 * <p>挂在 {@link CustomerMaterial} 下面：表头已经说了「这个客户怎么叫这个物料」，行只补「哪个SKU又不一样」。
 * 整条系列一个号的客户，这张子表是空的。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "CustomerVariant",
    indexes = {
      @Index(name = "IX_CustomerVariant_CustomerMaterialId", columnList = "CustomerMaterialId"),
      @Index(name = "IX_CustomerVariant_VariantId", columnList = "VariantId")
      // 注意：CustomerMaterialId + VariantId 的过滤唯一索引带 WHERE IsDeleted=0，JPA 不支持，用 Flyway 创建
    })
public class CustomerVariant extends BaseDeleteEntity {

  /** 客户物料Id */
  @Column(name = "CustomerMaterialId")
  private Long customerMaterialId;

  /** SKU Id */
  @Column(name = "VariantId", nullable = false)
  private Long variantId;

  /** 客户SKU代码（可空）：这个SKU对方怎么编号 */
  @Nationalized
  @Column(name = "CustomerVariantCode", length = 100)
  @Nullable
  private String customerVariantCode;

  /**
   * 构造器
   *
   * @param variantId SKU Id
   * @param customerVariantCode 客户SKU代码（可空）
   */
  CustomerVariant(long variantId, @Nullable String customerVariantCode) {
    if (variantId <= 0) {
      throw new DomainException("SKU不能为空");
    }
    this.variantId = variantId;
    apply(customerVariantCode);
  }

  /**
   * 修改客户物料行
   *
   * @param customerVariantCode 客户SKU代码（可空）
   */
  void modify(@Nullable String customerVariantCode) {
    apply(customerVariantCode);
  }

  // ==================== 私有：校验与赋值 ====================

  /**
   * 统一的可变字段校验与赋值入口（构造器与修改共用）
   *
   * @param customerVariantCode 客户SKU代码（可空）
   */
  private void apply(@Nullable String customerVariantCode) {
    this.customerVariantCode = StringUtils.trimToNull(customerVariantCode);
  }
}
