package com.rongia.basedata.domain.customermaterial;

import com.rongia.basedata.domain.customer.Customer;
import com.rongia.basedata.domain.customer.CustomerRepository;
import com.rongia.basedata.domain.customergroup.CustomerGroupRepository;
import com.rongia.basedata.domain.material.MaterialRepository;
import com.rongia.basedata.domain.variant.Variant;
import com.rongia.basedata.domain.variant.VariantRepository;
import com.rongia.shared.domain.exception.DomainException;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 客户物料领域服务，负责跨聚合编排 */
@NullMarked
public class CustomerMaterialDomainService {

  /** 客户物料工厂 */
  private final CustomerMaterialFactory customerMaterialFactory;

  /** 客户物料仓储 */
  private final CustomerMaterialRepository customerMaterialRepository;

  /** 客户仓储 */
  private final CustomerRepository customerRepository;

  /** 客户组仓储 */
  private final CustomerGroupRepository customerGroupRepository;

  /** 物料仓储 */
  private final MaterialRepository materialRepository;

  /** SKU 仓储 */
  private final VariantRepository variantRepository;

  /**
   * 构造器注入
   *
   * @param customerMaterialFactory 客户物料工厂
   * @param customerMaterialRepository 客户物料仓储
   * @param customerRepository 客户仓储
   * @param customerGroupRepository 客户组仓储
   * @param materialRepository 物料仓储
   * @param variantRepository SKU 仓储
   */
  public CustomerMaterialDomainService(
      CustomerMaterialFactory customerMaterialFactory,
      CustomerMaterialRepository customerMaterialRepository,
      CustomerRepository customerRepository,
      CustomerGroupRepository customerGroupRepository,
      MaterialRepository materialRepository,
      VariantRepository variantRepository) {
    this.customerMaterialFactory = customerMaterialFactory;
    this.customerMaterialRepository = customerMaterialRepository;
    this.customerRepository = customerRepository;
    this.customerGroupRepository = customerGroupRepository;
    this.materialRepository = materialRepository;
    this.variantRepository = variantRepository;
  }

  /**
   * 新建客户物料
   *
   * @param customerGroupId 客户组Id（可空）
   * @param customerId 客户Id（可空）
   * @param materialId 物料系列Id
   * @param customerMaterialCode 客户物料代码（可空）
   * @param customerMaterialName 客户物料名称（可空）
   * @return 客户物料聚合
   * @throws DomainException 客户或物料不存在、或对照已存在时抛出
   */
  public CustomerMaterial create(
      @Nullable Long customerGroupId,
      @Nullable Long customerId,
      long materialId,
      @Nullable String customerMaterialCode,
      @Nullable String customerMaterialName) {
    if (customerId == null && customerGroupId == null) {
      throw new DomainException("客户与客户组至少填一个");
    }
    if (customerId != null) {
      validateCustomer(customerId);
    }
    if (customerGroupId != null) {
      validateCustomerGroup(customerGroupId);
    }
    validateMaterial(materialId);
    return customerMaterialFactory.create(
        customerGroupId, customerId, materialId, customerMaterialCode, customerMaterialName);
  }

  /**
   * 修改客户物料
   *
   * @param id 客户物料Id
   * @param customerMaterialCode 客户物料代码（可空）
   * @param customerMaterialName 客户物料名称（可空）
   * @return 客户物料聚合
   * @throws DomainException 客户物料不存在时抛出
   */
  public CustomerMaterial modify(
      long id,
      @Nullable String customerMaterialCode,
      @Nullable String customerMaterialName) {
    CustomerMaterial model = getExist(id);
    model.modify(customerMaterialCode, customerMaterialName);
    return model;
  }

  /**
   * 添加SKU级对照
   *
   * @param id 客户物料Id
   * @param variantId SKU Id
   * @param customerVariantCode 客户SKU代码（可空）
   * @return 客户物料聚合
   * @throws DomainException 客户物料不存在、SKU 不存在或不属于该物料、或这个SKU已在对照里时抛出
   */
  public CustomerMaterial addItem(
      long id, long variantId, @Nullable String customerVariantCode) {
    CustomerMaterial model = getExist(id);
    validateVariant(model.getMaterialId(), variantId);
    model.addItem(variantId, customerVariantCode);
    return model;
  }

  /**
   * 修改SKU级对照
   *
   * @param id 客户物料Id
   * @param customerVariantId 客户物料行Id
   * @param customerVariantCode 客户SKU代码（可空）
   * @return 客户物料聚合
   * @throws DomainException 客户物料或行不存在时抛出
   */
  public CustomerMaterial modifyItem(
      long id, long customerVariantId, @Nullable String customerVariantCode) {
    CustomerMaterial model = getExist(id);
    model.modifyItem(customerVariantId, customerVariantCode);
    return model;
  }

  /**
   * 删除SKU级对照（逻辑删除）
   *
   * @param id 客户物料Id
   * @param customerVariantId 客户物料行Id
   * @return 客户物料聚合
   * @throws DomainException 客户物料不存在时抛出
   */
  public CustomerMaterial deleteItem(long id, long customerVariantId) {
    CustomerMaterial model = getExist(id);
    model.deleteItem(customerVariantId);
    return model;
  }

  /**
   * 删除客户物料（逻辑删除）
   *
   * @param id 客户物料Id
   * @return 客户物料聚合（已标记删除）
   * @throws DomainException 客户物料不存在时抛出
   */
  public CustomerMaterial delete(long id) {
    CustomerMaterial model = getExist(id);
    model.delete();
    return model;
  }

  /**
   * 取客户料号：先按客户找，找不到再按客户组兜底
   *
   * <p>客户自己配过就用自己的，没配过就用所属客户组的；两边都没有返回 {@code null}，表示这个物料在对方那儿
   * 没有专门编号，按我们的物料代码走。
   *
   * @param customerId 客户Id
   * @param materialId 物料系列Id
   * @return 客户物料代码（可空）
   * @throws DomainException 客户不存在时抛出
   */
  @Nullable
  public String getCustomerMaterialCode(long customerId, long materialId) {
    List<CustomerMaterial> own =
        customerMaterialRepository.getListByCustomerIdAndMaterialId(customerId, materialId);
    if (!own.isEmpty()) {
      return own.getFirst().getCustomerMaterialCode();
    }
    Customer customer = customerRepository.get(customerId);
    if (customer == null) {
      throw new DomainException("客户" + customerId + "不存在!");
    }
    Long customerGroupId = customer.getCustomerGroupId();
    if (customerGroupId == null) {
      return null;
    }
    List<CustomerMaterial> byGroup =
        customerMaterialRepository.getListByCustomerGroupIdAndMaterialId(
            customerGroupId, materialId);
    return byGroup.isEmpty() ? null : byGroup.getFirst().getCustomerMaterialCode();
  }

  /**
   * 按Id取客户物料
   *
   * @param id 客户物料Id
   * @return 客户物料聚合
   * @throws DomainException 客户物料不存在时抛出
   */
  private CustomerMaterial getExist(long id) {
    CustomerMaterial model = customerMaterialRepository.get(id);
    if (model == null) {
      throw new DomainException("客户物料" + id + "不存在!");
    }
    return model;
  }

  /**
   * 校验物料存在
   *
   * @param materialId 物料系列Id
   * @throws DomainException 物料不存在时抛出
   */
  private void validateMaterial(long materialId) {
    if (materialRepository.get(materialId) == null) {
      throw new DomainException("物料" + materialId + "不存在!");
    }
  }

  /**
   * 校验客户存在
   *
   * @param customerId 客户Id
   * @throws DomainException 客户不存在时抛出
   */
  private void validateCustomerGroup(long customerGroupId) {
    if (customerGroupRepository.get(customerGroupId) == null) {
      throw new DomainException("客户组" + customerGroupId + "不存在!");
    }
  }

  /**
   * 校验客户存在
   *
   * @param customerId 客户Id
   * @throws DomainException 客户不存在时抛出
   */
  private void validateCustomer(long customerId) {
    if (customerRepository.get(customerId) == null) {
      throw new DomainException("客户" + customerId + "不存在!");
    }
  }

  /**
   * 校验 SKU 存在且属于该物料
   *
   * @param materialId 物料系列Id
   * @param variantId SKU Id
   * @throws DomainException SKU 不存在或不属于该物料时抛出
   */
  private void validateVariant(long materialId, long variantId) {
    Variant variant = variantRepository.get(variantId);
    if (variant == null) {
      throw new DomainException("SKU" + variantId + "不存在!");
    }
    if (!Objects.equals(variant.getMaterialId(), materialId)) {
      throw new DomainException("SKU" + variantId + "不属于物料" + materialId + "!");
    }
  }
}
