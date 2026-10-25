package com.rongia.system.domain.company;

import com.rongia.shared.domain.aggregate.UpdateAggregateRoot;
import com.rongia.shared.domain.entity.Parent;
import com.rongia.shared.domain.exception.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Nationalized;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 公司（集团下属分工厂）
 *
 * <p>字段照别家的公司主数据取：ERPNext {@code Company} 的 {@code parent_company}（上级公司）、{@code tax_id}、 {@code
 * date_of_establishment}、{@code default_currency}（必填）；Odoo {@code res.company} 的 {@code
 * company_registry}（工商注册号）、{@code currency_id}（必填）；金蝶「组织机构」的上级组织与禁用。
 *
 * <p>地址与 {@code Supplier} 同形：{@code Country/RegionCode/Region/Zip} + 详细地址一段（{@code Address}），
 * 便于以后按省市统计和打印。
 *
 * <p>代码与简称是两件事，别混用：代码是给机器认的稳定串（导入对码、以后拼单号前缀），改了要断历史，所以全司唯一且必填—— SAP 的公司代码 {@code BUKRS}（4
 * 位，本身就是主键，名称另存 {@code BUTXT}）、D365 的法人编号（{@code USMF} 这种）、 ERPNext 的 {@code
 * abbr}（必填，会被拼进科目、仓库、成本中心编码前）、金蝶组织机构的 {@code FNumber} 都是这一列； Odoo 是唯一没有代码列的，它靠 {@code partner_id}
 * 把公司指成一个联系人、用 {@code company_registry} 当对外唯一号。 简称是给人在单据页眉上看的短名，想改随时能改，可空。
 *
 * <p>上级公司构成一棵树，层级路径由 {@link CompanyDomainService} 维护（与 {@code Category}、{@code Department}
 * 同一套）；往返、跨公司校验放在领域服务里。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "Company",
    indexes = {
      @Index(name = "IX_Company_ParentId", columnList = "ParentId"),
      @Index(name = "IX_Company_ParentPath", columnList = "ParentPath"),
      @Index(name = "IX_Company_Code", columnList = "Code"),
      @Index(name = "IX_Company_Name", columnList = "Name"),
      @Index(name = "IX_Company_Sequence", columnList = "Sequence")
      // 注意：Code 的过滤唯一索引带 WHERE IsDeleted=0，用 Flyway 创建
      // 注意：同级公司名称唯一由 CompanyRepository.isExistByNameAndParentIdExcludeId 校验
    })
public class Company extends UpdateAggregateRoot implements Parent {

  /** 上级公司Id（可空，空表示顶级） */
  @Column(name = "ParentId")
  @Nullable
  private Long parentId;

  /** 层级路径（可空，形如 "1-3-5"，由层级编排逻辑维护） */
  @Nationalized
  @Column(name = "ParentPath")
  @Nullable
  private String parentPath;

  /** 公司代码 */
  @Nationalized
  @Column(name = "Code", nullable = false, length = 100)
  private String code;

  /** 名称 */
  @Nationalized
  @Column(name = "Name", nullable = false, length = 200)
  private String name;

  /** 简称（可空），单据页眉与编号前缀用 */
  @Nationalized
  @Column(name = "ShortName", length = 100)
  @Nullable
  private String shortName;

  /** 税务登记号（可空） */
  @Nationalized
  @Column(name = "TaxRegisterCode", length = 200)
  @Nullable
  private String taxRegisterCode;

  /** 工商注册号（可空） */
  @Nationalized
  @Column(name = "RegistryCode", length = 200)
  @Nullable
  private String registryCode;

  /** 成立日期（可空） */
  @Column(name = "EstablishedDate")
  @Nullable
  private LocalDate establishedDate;

  /** 默认币别Id */
  @Column(name = "CurrencyId", nullable = false)
  private Long currencyId;

  /** 电话号码（可空） */
  @Nationalized
  @Column(name = "Phone", length = 50)
  @Nullable
  private String phone;

  /** 邮箱（可空） */
  @Nationalized
  @Column(name = "Email", length = 200)
  @Nullable
  private String email;

  /** 国家（可空） */
  @Nationalized
  @Column(name = "Country", length = 50)
  @Nullable
  private String country;

  /** 地区编码（可空）：区县一级的行政区划代码（如 440106），省市区三级都在这六位里 */
  @Nationalized
  @Column(name = "RegionCode", length = 100)
  @Nullable
  private String regionCode;

  /** 地区全称（可空）：如「广东省广州市天河区」，打印直接用它 */
  @Nationalized
  @Column(name = "Region", length = 200)
  @Nullable
  private String region;

  /** 邮政编码（可空） */
  @Nationalized
  @Column(name = "Zip", length = 20)
  @Nullable
  private String zip;

  /** 详细地址（可空） */
  @Nationalized
  @Column(name = "Address", length = 500)
  @Nullable
  private String address;

  /** 排序（可空） */
  @Column(name = "Sequence")
  @Nullable
  private Integer sequence;

  /** 是否禁用 */
  @Column(name = "Disabled", nullable = false)
  private Boolean disabled = false;

  /** 备注（可空） */
  @Nationalized
  @Column(name = "Remark", length = 500)
  @Nullable
  private String remark;

  /**
   * 构造器
   *
   * @param code 公司代码
   * @param name 名称
   * @param shortName 简称（可空）
   * @param parentId 上级公司Id（可空）
   * @param parentPath 层级路径（可空）
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
   */
  Company(
      String code,
      String name,
      @Nullable String shortName,
      @Nullable Long parentId,
      @Nullable String parentPath,
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
    apply(
        code,
        name,
        shortName,
        parentId,
        parentPath,
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
   * 修改公司
   *
   * @param code 公司代码
   * @param name 名称
   * @param shortName 简称（可空）
   * @param parentId 上级公司Id（可空）
   * @param parentPath 层级路径（可空）
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
   */
  public void modify(
      String code,
      String name,
      @Nullable String shortName,
      @Nullable Long parentId,
      @Nullable String parentPath,
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
    apply(
        code,
        name,
        shortName,
        parentId,
        parentPath,
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
   * 重设层级路径（由层级编排逻辑调用）
   *
   * @param parentPath 层级路径（可空）
   */
  @Override
  public void changeParentPath(@Nullable String parentPath) {
    this.parentPath = parentPath;
  }

  // ==================== 私有：校验与赋值 ====================

  /**
   * 统一的校验与赋值入口（构造器与修改共用）
   *
   * @param code 公司代码
   * @param name 名称
   * @param shortName 简称（可空）
   * @param parentId 上级公司Id（可空）
   * @param parentPath 层级路径（可空）
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
   * @throws DomainException 代码/名称为空、或默认币别为空时抛出
   */
  private void apply(
      String code,
      String name,
      @Nullable String shortName,
      @Nullable Long parentId,
      @Nullable String parentPath,
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
    String trimmedCode = StringUtils.trimToNull(code);
    if (trimmedCode == null) {
      throw new DomainException("公司代码不能为空");
    }
    String trimmedName = StringUtils.trimToNull(name);
    if (trimmedName == null) {
      throw new DomainException("公司名称不能为空");
    }
    if (currencyId <= 0) {
      throw new DomainException("公司默认币别不能为空");
    }

    this.code = trimmedCode;
    this.name = trimmedName;
    this.shortName = shortName;
    this.parentId = parentId;
    this.parentPath = parentPath;
    this.phone = phone;
    this.email = email;
    this.country = country;
    this.regionCode = regionCode;
    this.region = region;
    this.zip = zip;
    this.address = address;
    this.taxRegisterCode = taxRegisterCode;
    this.registryCode = registryCode;
    this.establishedDate = establishedDate;
    this.currencyId = currencyId;
    this.disabled = disabled;
    this.sequence = sequence;
    this.remark = remark;
  }
}
