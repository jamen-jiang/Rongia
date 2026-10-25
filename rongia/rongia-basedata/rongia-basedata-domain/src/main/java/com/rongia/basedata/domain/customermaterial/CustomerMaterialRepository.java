package com.rongia.basedata.domain.customermaterial;

import com.rongia.shared.domain.repository.Repository;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 客户物料仓储接口 */
@NullMarked
public interface CustomerMaterialRepository extends Repository<CustomerMaterial> {

  /**
   * 按客户 + 物料系列查询客户物料
   *
   * @param customerId 客户Id
   * @param materialId 物料系列Id
   * @return 客户物料列表
   */
  List<CustomerMaterial> getListByCustomerIdAndMaterialId(long customerId, long materialId);

  /**
   * 按客户组 + 物料系列查询客户物料
   *
   * @param customerGroupId 客户组Id
   * @param materialId 物料系列Id
   * @return 客户物料列表
   */
  List<CustomerMaterial> getListByCustomerGroupIdAndMaterialId(
      long customerGroupId, long materialId);

  /**
   * 按客户查客户料号（客户报料号反查物料）
   *
   * @param customerId 客户Id
   * @param customerMaterialCode 客户物料代码
   * @return 客户物料列表
   */
  List<CustomerMaterial> getListByCustomerIdAndCustomerMaterialCode(
      long customerId, String customerMaterialCode);

  /**
   * 判断该客户这个物料是否已有对照（排除指定 Id，用于修改时排除自身）
   *
   * @param customerId 客户Id
   * @param materialId 物料系列Id
   * @param id 排除的Id（可空，新增时传 null）
   * @return true 表示已存在
   */
  boolean isExistByCustomerIdAndMaterialIdExcludeId(
      long customerId, long materialId, @Nullable Long id);

  /**
   * 判断该客户这个物料是否已有对照
   *
   * @param customerId 客户Id
   * @param materialId 物料系列Id
   * @return true 表示已存在
   */
  default boolean isExistByCustomerIdAndMaterialIdExcludeId(
      long customerId, long materialId) {
    return isExistByCustomerIdAndMaterialIdExcludeId(customerId, materialId, null);
  }

  /**
   * 判断该客户组这个物料是否已有对照（排除指定 Id，用于修改时排除自身）
   *
   * @param customerGroupId 客户组Id
   * @param materialId 物料系列Id
   * @param id 排除的Id（可空，新增时传 null）
   * @return true 表示已存在
   */
  boolean isExistByCustomerGroupIdAndMaterialIdExcludeId(
      long customerGroupId, long materialId, @Nullable Long id);

  /**
   * 判断该客户组这个物料是否已有对照
   *
   * @param customerGroupId 客户组Id
   * @param materialId 物料系列Id
   * @return true 表示已存在
   */
  default boolean isExistByCustomerGroupIdAndMaterialIdExcludeId(
      long customerGroupId, long materialId) {
    return isExistByCustomerGroupIdAndMaterialIdExcludeId(customerGroupId, materialId, null);
  }
}
