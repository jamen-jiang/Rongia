package com.rongia.basedata.domain.customermaterial;

import com.rongia.shared.domain.aggregate.UpdateAggregateRoot;
import com.rongia.shared.domain.exception.DomainException;
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
 * 客户物料（这个物料，某个客户那边叫什么）
 *
 * <p>表头说「这个物料、这个客户、整条系列对方怎么编号」；个别SKU对方另有编号的，加在
 * {@link CustomerVariant} 子行上。大多数客户整条系列一个号，子表就是空的。
 *
 * <p>价格不在这张表上：客户价是「客户 × 物料 × 有效期」的事，与料号无关，另开价格表。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "CustomerMaterial",
    indexes = {
      @Index(name = "IX_CustomerMaterial_CustomerGroupId", columnList = "CustomerGroupId"),
      @Index(name = "IX_CustomerMaterial_CustomerId", columnList = "CustomerId"),
      @Index(name = "IX_CustomerMaterial_MaterialId", columnList = "MaterialId"),
      @Index(name = "IX_CustomerMaterial_CustomerMaterialCode", columnList = "CustomerMaterialCode")
      // 注意：CustomerId + MaterialId 的过滤唯一索引带 WHERE IsDeleted=0，JPA 不支持，用 Flyway 创建
    })
public class CustomerMaterial extends UpdateAggregateRoot {

  /** 客户组Id（可空）：按组配的对照，具体客户查不到时兜底；与客户Id至少填一个 */
  @Column(name = "CustomerGroupId")
  @Nullable
  private Long customerGroupId;

  /** 客户Id（可空，不可变：换客户就是另一行）；与客户组Id至少填一个 */
  @Column(name = "CustomerId")
  @Nullable
  private Long customerId;

  /** 物料系列Id（不可变） */
  @Column(name = "MaterialId", nullable = false)
  private Long materialId;

  /** 客户物料代码（可空）：客户对这个物料的编号，下单与对账用 */
  @Nationalized
  @Column(name = "CustomerMaterialCode", length = 100)
  @Nullable
  private String customerMaterialCode;

  /** 客户物料名称（可空）：客户对这个物料的叫法 */
  @Nationalized
  @Column(name = "CustomerMaterialName", length = 200)
  @Nullable
  private String customerMaterialName;

  /** 客户SKU编号（个别SKU单独一套编号时才有） */
  @OneToMany(
      fetch = FetchType.EAGER,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE},
      orphanRemoval = true)
  @JoinColumn(name = "CustomerMaterialId")
  @SQLRestriction("IsDeleted = 0")
  private List<CustomerVariant> customerVariants = new ArrayList<>();

  /**
   * 构造器
   *
   * @param customerGroupId 客户组Id（可空）
   * @param customerId 客户Id
   * @param materialId 物料系列Id
   * @param customerMaterialCode 客户物料代码（可空）
   * @param customerMaterialName 客户物料名称（可空）
   */
  CustomerMaterial(
      @Nullable Long customerGroupId,
      @Nullable Long customerId,
      long materialId,
      @Nullable String customerMaterialCode,
      @Nullable String customerMaterialName) {
    if (materialId <= 0) {
      throw new DomainException("物料不能为空");
    }
    if (customerId == null && customerGroupId == null) {
      throw new DomainException("客户与客户组至少填一个");
    }
    if (customerId != null && customerId <= 0) {
      throw new DomainException("客户" + customerId + "有误!");
    }
    if (customerGroupId != null && customerGroupId <= 0) {
      throw new DomainException("客户组" + customerGroupId + "有误!");
    }
    this.customerGroupId = customerGroupId;
    this.customerId = customerId;
    this.materialId = materialId;
    apply(customerMaterialCode, customerMaterialName);
  }

  /**
   * 获取客户物料行（只读）
   *
   * @return 不可变列表
   */
  public List<CustomerVariant> getCustomerVariants() {
    return Collections.unmodifiableList(customerVariants);
  }

  /**
   * 修改客户物料
   *
   * @param customerMaterialCode 客户物料代码（可空）
   * @param customerMaterialName 客户物料名称（可空）
   */
  public void modify(@Nullable String customerMaterialCode, @Nullable String customerMaterialName) {
    apply(customerMaterialCode, customerMaterialName);
  }

  /**
   * 添加SKU级对照
   *
   * @param variantId SKU Id
   * @param customerVariantCode 客户SKU代码（可空）
   * @throws DomainException SKU 为空、或这个SKU已在本客户下时抛出
   */
  public void addItem(long variantId, @Nullable String customerVariantCode) {
    if (variantId <= 0) {
      throw new DomainException("SKU不能为空");
    }
    if (findItemByVariantId(variantId) != null) {
      throw new DomainException("这个SKU已在本客户的对照里!");
    }
    customerVariants.add(new CustomerVariant(variantId, customerVariantCode));
  }

  /**
   * 修改SKU级对照
   *
   * @param customerVariantId 客户物料行Id
   * @param customerVariantCode 客户SKU代码（可空）
   * @throws DomainException 行不存在时抛出
   */
  public void modifyItem(long customerVariantId, @Nullable String customerVariantCode) {
    requireItem(customerVariantId).modify(customerVariantCode);
  }

  /**
   * 删除SKU级对照（逻辑删除）
   *
   * @param customerVariantId 客户物料行Id
   */
  public void deleteItem(long customerVariantId) {
    CustomerVariant item = findItem(customerVariantId);
    if (item == null) {
      return;
    }
    item.delete();
  }

  // ==================== 私有：校验与赋值 ====================

  /**
   * 统一的可变字段校验与赋值入口（构造器与修改共用）
   *
   * @param customerMaterialCode 客户物料代码（可空）
   * @param customerMaterialName 客户物料名称（可空）
   */
  private void apply(@Nullable String customerMaterialCode, @Nullable String customerMaterialName) {
    this.customerMaterialCode = StringUtils.trimToNull(customerMaterialCode);
    this.customerMaterialName = StringUtils.trimToNull(customerMaterialName);
  }

  /**
   * 按行Id查找未删除的客户物料行
   *
   * @param customerVariantId 客户物料行Id
   * @return 匹配到的行，找不到返回 {@code null}
   */
  @Nullable
  private CustomerVariant findItem(long customerVariantId) {
    return customerVariants.stream()
        .filter(i -> Objects.equals(i.getId(), customerVariantId) && !i.getIsDeleted())
        .findFirst()
        .orElse(null);
  }

  /**
   * 按SKU查找未删除的客户物料行
   *
   * @param variantId SKU Id
   * @return 匹配到的行，找不到返回 {@code null}
   */
  @Nullable
  private CustomerVariant findItemByVariantId(long variantId) {
    return customerVariants.stream()
        .filter(i -> Objects.equals(i.getVariantId(), variantId) && !i.getIsDeleted())
        .findFirst()
        .orElse(null);
  }

  /**
   * 取客户物料行，不存在则抛异常
   *
   * @param customerVariantId 客户物料行Id
   * @return 客户物料行
   * @throws DomainException 行不存在时抛出
   */
  private CustomerVariant requireItem(long customerVariantId) {
    CustomerVariant item = findItem(customerVariantId);
    if (item == null) {
      throw new DomainException("客户物料行不存在!");
    }
    return item;
  }
}
