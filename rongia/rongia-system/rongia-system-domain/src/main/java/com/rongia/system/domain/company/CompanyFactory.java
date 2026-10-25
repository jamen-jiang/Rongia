package com.rongia.system.domain.company;

import com.rongia.shared.domain.exception.DomainException;
import com.rongia.shared.domain.util.HierarchyUtils;
import java.time.LocalDate;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 公司工厂，负责创建公司 */
@NullMarked
public class CompanyFactory {

  /** 公司仓储 */
  private final CompanyRepository companyRepository;

  /**
   * 构造器注入
   *
   * @param companyRepository 公司仓储
   */
  public CompanyFactory(CompanyRepository companyRepository) {
    this.companyRepository = companyRepository;
  }

  /**
   * 创建公司
   *
   * @param code 公司代码
   * @param name 公司名称
   * @param shortName 简称（可空）
   * @param parentId 上级公司Id（可空）
   * @param phone 电话号码（可空）
   * @param email 邮箱（可空）
   * @param country 国家（可空）
   * @param regionCode 地区编码（可空）：区县一级的行政区划代码，省市区都在这六位里
   * @param region 地区全称（可空）：如「广东省广州市天河区」
   * @param zip 邮政编码（可空）
   * @param address 详细地址（可空）
   * @param taxRegisterCode 税务登记号（可空）
   * @param registryCode 工商注册号（可空）
   * @param establishedDate 成立日期（可空）
   * @param currencyId 默认币别Id
   * @param disabled 是否禁用
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   * @return 公司聚合
   * @throws DomainException 公司代码已存在、同级公司名称已存在、或上级公司不存在时抛出
   */
  public Company create(
      String code,
      String name,
      @Nullable String shortName,
      @Nullable Long parentId,
      @Nullable String phone,
      @Nullable String email,
      @Nullable String country,
      @Nullable String regionCode,
      @Nullable String region,
      @Nullable String zip,
      @Nullable String address,
      @Nullable String taxRegisterCode,
      @Nullable String registryCode,
      @Nullable LocalDate establishedDate,
      long currencyId,
      boolean disabled,
      @Nullable Integer sequence,
      @Nullable String remark) {
    // 校验代码全司唯一、同级公司名称唯一
    if (companyRepository.isExistByCodeExcludeId(code)) {
      throw new DomainException("公司代码" + code + "已存在!");
    }
    if (companyRepository.isExistByNameAndParentIdExcludeId(name, parentId)) {
      throw new DomainException("同级公司名称" + name + "已存在!");
    }
    return new Company(
        code,
        name,
        shortName,
        parentId,
        HierarchyUtils.resolveParentPath(parentId, getParent(parentId), "上级公司"),
        phone,
        email,
        country,
        regionCode,
        region,
        zip,
        address,
        taxRegisterCode,
        registryCode,
        establishedDate,
        currencyId,
        disabled,
        sequence,
        remark);
  }

  /**
   * 查询上级公司
   *
   * @param parentId 上级公司Id（可空）
   * @return 上级公司，无父Id或不存在时返回 null（可空）
   */
  @Nullable
  private Company getParent(@Nullable Long parentId) {
    return parentId == null ? null : companyRepository.get(parentId);
  }
}
