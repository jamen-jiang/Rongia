package com.rongia.basedata.domain.customer;

import com.rongia.basedata.domain.currency.CurrencyRepository;
import com.rongia.shared.domain.exception.DomainException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 客户领域服务，负责跨聚合编排 */
@NullMarked
public class CustomerDomainService {

  /** 客户工厂 */
  private final CustomerFactory customerFactory;

  /** 客户仓储 */
  private final CustomerRepository customerRepository;

  /** 币种仓储 */
  private final CurrencyRepository currencyRepository;

  /**
   * 构造器注入
   *
   * @param customerFactory 客户工厂
   * @param customerRepository 客户仓储
   * @param currencyRepository 币种仓储
   */
  public CustomerDomainService(
      CustomerFactory customerFactory,
      CustomerRepository customerRepository,
      CurrencyRepository currencyRepository) {
    this.customerFactory = customerFactory;
    this.customerRepository = customerRepository;
    this.currencyRepository = currencyRepository;
  }

  /**
   * 创建客户
   *
   * @param customerGroupId 客户组Id（可空）
   * @param code 客户代码
   * @param name 客户名称
   * @param shortName 客户简称（可空）
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
   * @return 客户聚合
   * @throws DomainException 币种不存在、或代码已存在时抛出
   */
  public Customer create(
      @Nullable Long customerGroupId,
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
    return customerFactory.create(
        customerGroupId,
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
   * 导入生成：客户存在则修改，不存在则创建
   *
   * @param customerGroupId 客户组Id（可空）
   * @param code 客户代码
   * @param name 客户名称
   * @param shortName 客户简称（可空）
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
   * @return 客户聚合
   * @throws DomainException 引用的主数据不存在、或代码冲突时抛出
   */
  public Customer importGenerate(
      @Nullable Long customerGroupId,
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
    // 按代码查客户
    Customer model = customerRepository.getByCode(code);

    // 不存在则创建，存在则修改
    if (model == null) {
      model =
          create(
              customerGroupId,
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
          customerGroupId,
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
   * 修改客户（按Id查询后修改）
   *
   * @param id 客户Id
   * @param customerGroupId 客户组Id（可空）
   * @param code 客户代码
   * @param name 客户名称
   * @param shortName 客户简称（可空）
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
   * @return 客户聚合
   * @throws DomainException 客户不存在时抛出
   */
  public Customer modify(
      long id,
      @Nullable Long customerGroupId,
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
    // 1. 查客户
    Customer model = customerRepository.get(id);
    if (model == null) {
      throw new DomainException("客户不存在!");
    }
    // 2. 委托给使用已有聚合的重载
    modify(
        model,
        customerGroupId,
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
   * 修改客户（使用已有聚合，避免重复查询）
   *
   * @param customer 客户聚合
   * @param customerGroupId 客户组Id（可空）
   * @param code 客户代码
   * @param name 客户名称
   * @param shortName 客户简称（可空）
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
      Customer customer,
      @Nullable Long customerGroupId,
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
    // 2. 校验客户代码唯一（排除自身）
    if (customerRepository.isExistByCodeExcludeId(code, customer.getId())) {
      throw new DomainException("客户代码" + code + "已存在!");
    }
    // 3. 修改主字段
    customer.modify(
        customerGroupId,
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
