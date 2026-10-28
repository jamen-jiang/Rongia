package com.rongia.purchase.domain.purchaseorder;

import com.rongia.shared.domain.repository.Repository;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 采购单仓储接口 */
@NullMarked
public interface PurchaseOrderRepository extends Repository<PurchaseOrder> {

  /**
   * 按单号查询
   *
   * @param number 采购单号
   * @return 采购单（可空），不存在返回 null
   */
  @Nullable
  PurchaseOrder getByNumber(String number);

  /**
   * 按供应商查询采购单
   *
   * @param supplierId 供应商Id
   * @return 采购单列表
   */
  List<PurchaseOrder> getListBySupplierId(long supplierId);

  /**
   * 判断单号是否存在
   *
   * @param number 采购单号
   * @param id 排除的采购单Id（可空，新增时传 null）
   * @return true 表示已存在
   */
  boolean isExistByNumberExcludeId(String number, @Nullable Long id);

  /**
   * 判断单号是否存在
   *
   * @param number 采购单号
   * @return true 表示已存在
   */
  default boolean isExistByNumberExcludeId(String number) {
    return isExistByNumberExcludeId(number, null);
  }
}
