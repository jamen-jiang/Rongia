package com.rongia.basedata.domain.supplier;

import com.rongia.shared.domain.exception.DomainException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 供应商工厂，负责创建供应商 */
@NullMarked
public class SupplierFactory {

  /** 供应商仓储 */
  private final SupplierRepository supplierRepository;

  /**
   * 构造器注入
   *
   * @param supplierRepository 供应商仓储
   */
  public SupplierFactory(SupplierRepository supplierRepository) {
    this.supplierRepository = supplierRepository;
  }

  /**
   * 创建供应商
   *
   * @param code 供应商代码
   * @param name 供应商名称
   * @param shortName 供应商简称（可空）
   * @param taxRegisterCode 税务登记号（可空）
   * @param currencyId 结算币别Id
   * @param taxRateId 税率Id（可空）
   * @param phone 电话号码（可空）
   * @param email 邮箱（可空）
   * @param country 国家（可空）
   * @param regionCode 地区编码（可空）：区县一级的行政区划代码，省市区都在这六位里
   * @param region 地区全称（可空）：如「广东省广州市天河区」
   * @param zip 邮政编码（可空）
   * @param address 详细地址（可空）
   * @param disabled 是否停用
   * @param remark 备注（可空）
   * @return 供应商聚合
   * @throws DomainException 供应商代码已存在时抛出
   */
  public Supplier create(
      String code,
      String name,
      @Nullable String shortName,
      @Nullable String taxRegisterCode,
      long currencyId,
      @Nullable Long taxRateId,
      @Nullable String phone,
      @Nullable String email,
      @Nullable String country,
      @Nullable String regionCode,
      @Nullable String region,
      @Nullable String zip,
      @Nullable String address,
      boolean disabled,
      @Nullable String remark) {
    // 校验供应商代码唯一
    if (supplierRepository.isExistByCodeExcludeId(code)) {
      throw new DomainException("供应商代码" + code + "已存在!");
    }
    return new Supplier(
        code,
        name,
        shortName,
        taxRegisterCode,
        currencyId,
        taxRateId,
        phone,
        email,
        country,
        regionCode,
        region,
        zip,
        address,
        disabled,
        remark);
  }
}
