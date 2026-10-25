package com.rongia.basedata.domain.customermaterial;

import com.rongia.shared.domain.exception.DomainException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 客户物料工厂，负责创建客户物料 */
@NullMarked
public class CustomerMaterialFactory {

  /** 客户物料仓储 */
  private final CustomerMaterialRepository customerMaterialRepository;

  /**
   * 构造器注入
   *
   * @param customerMaterialRepository 客户物料仓储
   */
  public CustomerMaterialFactory(CustomerMaterialRepository customerMaterialRepository) {
    this.customerMaterialRepository = customerMaterialRepository;
  }

  /**
   * 创建客户物料
   *
   * @param customerGroupId 客户组Id（可空）
   * @param customerId 客户Id（可空）
   * @param materialId 物料系列Id
   * @param customerMaterialCode 客户物料代码（可空）
   * @param customerMaterialName 客户物料名称（可空）
   * @return 客户物料聚合
   * @throws DomainException 这个客户（或这个客户组）这个物料已经有对照时抛出
   */
  public CustomerMaterial create(
      @Nullable Long customerGroupId,
      @Nullable Long customerId,
      long materialId,
      @Nullable String customerMaterialCode,
      @Nullable String customerMaterialName) {
    if (customerId != null
        && customerMaterialRepository.isExistByCustomerIdAndMaterialIdExcludeId(
            customerId, materialId)) {
      throw new DomainException("这个客户这个物料的对照已经存在!");
    }
    if (customerGroupId != null
        && customerMaterialRepository.isExistByCustomerGroupIdAndMaterialIdExcludeId(
            customerGroupId, materialId)) {
      throw new DomainException("这个客户组这个物料的对照已经存在!");
    }
    return new CustomerMaterial(
        customerGroupId, customerId, materialId, customerMaterialCode, customerMaterialName);
  }
}
