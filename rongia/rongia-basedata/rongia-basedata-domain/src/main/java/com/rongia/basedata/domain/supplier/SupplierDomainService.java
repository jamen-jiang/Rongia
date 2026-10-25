package com.rongia.basedata.domain.supplier;

import com.rongia.basedata.domain.currency.CurrencyRepository;
import com.rongia.shared.domain.exception.DomainException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 供应商领域服务，负责跨聚合编排 */
@NullMarked
public class SupplierDomainService {

  /** 供应商工厂 */
  private final SupplierFactory supplierFactory;

  /** 供应商仓储 */
  private final SupplierRepository supplierRepository;

  /** 币种仓储 */
  private final CurrencyRepository currencyRepository;

  /**
   * 构造器注入
   *
   * @param supplierFactory 供应商工厂
   * @param supplierRepository 供应商仓储
   * @param currencyRepository 币种仓储
   */
  public SupplierDomainService(
      SupplierFactory supplierFactory,
      SupplierRepository supplierRepository,
      CurrencyRepository currencyRepository) {
    this.supplierFactory = supplierFactory;
    this.supplierRepository = supplierRepository;
    this.currencyRepository = currencyRepository;
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
   * @throws DomainException 币种不存在、或代码已存在时抛出
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
    validateCurrency(currencyId);
    return supplierFactory.create(
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

  /**
   * 导入生成：供应商存在则修改，不存在则创建
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
   * @throws DomainException 引用的主数据不存在、或代码冲突时抛出
   */
  public Supplier importGenerate(
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
    // 按代码查供应商
    Supplier model = supplierRepository.getByCode(code);

    // 不存在则创建，存在则修改
    if (model == null) {
      model =
          create(
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
    } else {
      modify(
          model,
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
    return model;
  }

  /**
   * 修改供应商（按Id查询后修改）
   *
   * @param id 供应商Id
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
   * @throws DomainException 供应商不存在时抛出
   */
  public Supplier modify(
      long id,
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
    // 1. 查供应商
    Supplier model = supplierRepository.get(id);
    if (model == null) {
      throw new DomainException("供应商不存在!");
    }
    // 2. 委托给使用已有聚合的重载
    modify(
        model,
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
    return model;
  }

  /**
   * 修改供应商（使用已有聚合，避免重复查询）
   *
   * @param supplier 供应商聚合
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
   * @throws DomainException 引用的主数据不存在、或代码冲突时抛出
   */
  public void modify(
      Supplier supplier,
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
    // 1. 引用的主数据要存在
    validateCurrency(currencyId);
    // 2. 校验供应商代码唯一（排除自身）
    if (supplierRepository.isExistByCodeExcludeId(code, supplier.getId())) {
      throw new DomainException("供应商代码" + code + "已存在!");
    }
    // 3. 修改主字段
    supplier.modify(
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

  // ==================== 私有：校验 ====================

  /**
   * 校验引用的币种存在
   *
   * @param currencyId 结算币别Id
   * @throws DomainException 币种不存在时抛出
   */
  private void validateCurrency(long currencyId) {
    if (currencyRepository.get(currencyId) == null) {
      throw new DomainException("币种" + currencyId + "不存在!");
    }
  }
}
