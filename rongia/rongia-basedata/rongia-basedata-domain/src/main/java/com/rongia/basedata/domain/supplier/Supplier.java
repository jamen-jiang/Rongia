package com.rongia.basedata.domain.supplier;

import com.rongia.shared.domain.aggregate.UpdateAggregateRoot;
import com.rongia.shared.domain.exception.DomainException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
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
 * 供应商
 *
 * <p>结算币别、税率是引用不是文本：金蝶供应商财务页签（结算币别 {@code FPayCurrencyId} /
 * 默认税率 {@code FTaxRateId}）、D365 的 Currency 都是引用。存文本的话，采购单取不到默认值。
 *
 * <p>拦不拦交易只有一个 {@code disabled}：勾了就表示这个供应商不再用，采购单、询价、发票付款都不许再选它。
 * 系统里没有「资料照用、只停某个环节」的流程（没有 POS，解冻定时任务也不存在），所以不预置冻结范围、
 * 冻结日期这类字段——等真出现按环节拦截的需求，连同判断它的单据一起加。
 *
 * <p>银行账户不在这里，见 {@link com.rongia.basedata.domain.bankaccount.BankAccount}：它是通用聚合，
 * 用「归属类型 + 归属Id」挂到供应商上，一个供应商可以有多个账户。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "Supplier",
    indexes = {
      @Index(name = "IX_Supplier_Code", columnList = "Code"),
      @Index(name = "IX_Supplier_Name", columnList = "Name")
      // 注意：Code 的过滤唯一索引带 WHERE IsDeleted=0，JPA 不支持，用 Flyway 创建
    })
public class Supplier extends UpdateAggregateRoot {

  /** 供应商代码 */
  @Nationalized
  @Column(name = "Code", nullable = false, length = 100)
  private String code;

  /** 供应商名称 */
  @Nationalized
  @Column(name = "Name", nullable = false, length = 200)
  private String name;

  /** 供应商简称（可空） */
  @Nationalized
  @Column(name = "ShortName", length = 100)
  @Nullable
  private String shortName;

  /** 税务登记号（可空） */
  @Nationalized
  @Column(name = "TaxRegisterCode", length = 200)
  @Nullable
  private String taxRegisterCode;

  /** 结算币别Id */
  @Column(name = "CurrencyId", nullable = false)
  private Long currencyId;

  /** 税率Id（可空） */
  @Column(name = "TaxRateId")
  @Nullable
  private Long taxRateId;

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

  /** 是否停用 */
  @Column(name = "Disabled", nullable = false)
  private Boolean disabled;

  /** 备注（可空） */
  @Nationalized
  @Column(name = "Remark", length = 500)
  @Nullable
  private String remark;

  /** 联系人列表（子表，一个供应商多个联系人） */
  @OneToMany(
      fetch = FetchType.EAGER,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE},
      orphanRemoval = true)
  @JoinColumn(name = "SupplierId")
  @SQLRestriction("IsDeleted = 0")
  private List<SupplierContact> supplierContacts = new ArrayList<>();

  /**
   * 获取联系人列表（只读）
   *
   * @return 不可变视图
   */
  public List<SupplierContact> getSupplierContacts() {
    return Collections.unmodifiableList(supplierContacts);
  }

  /**
   * 新增联系人
   *
   * @param name 姓名
   * @param phone 电话（可空）
   * @param mobile 手机（可空）
   * @param email 邮箱（可空）
   * @param remark 备注（可空）
   */
  public void addContact(
      String name,
      @Nullable String phone,
      @Nullable String mobile,
      @Nullable String email,
      @Nullable String remark) {
    supplierContacts.add(new SupplierContact(name, phone, mobile, email, remark));
  }

  /**
   * 修改联系人
   *
   * @param contactId 联系人Id
   * @param name 姓名
   * @param phone 电话（可空）
   * @param mobile 手机（可空）
   * @param email 邮箱（可空）
   * @param remark 备注（可空）
   * @throws DomainException 联系人不存在时抛出
   */
  public void modifyContact(
      long contactId,
      String name,
      @Nullable String phone,
      @Nullable String mobile,
      @Nullable String email,
      @Nullable String remark) {
    SupplierContact contact = findContact(contactId);
    if (contact == null) {
      throw new DomainException("供应商联系人不存在!");
    }
    contact.modify(name, phone, mobile, email, remark);
  }

  /**
   * 删除联系人（逻辑删除）
   *
   * <p>找不到时静默返回，保证删除幂等。
   *
   * @param contactId 联系人Id
   */
  public void deleteContact(long contactId) {
    SupplierContact contact = findContact(contactId);
    if (contact != null) {
      contact.delete();
    }
  }

  /**
   * 按Id查联系人（排除已删除）
   *
   * @param contactId 联系人Id
   * @return 联系人（可空）
   */
  @Nullable
  private SupplierContact findContact(long contactId) {
    return supplierContacts.stream()
        .filter(x -> Objects.equals(x.getId(), contactId) && !x.getIsDeleted())
        .findFirst()
        .orElse(null);
  }

  /**
   * 构造器
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
   */
  Supplier(
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
    apply(
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
   * 修改供应商
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
   */
  void modify(
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
    apply(
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

  // ==================== 私有：校验与赋值 ====================

  /**
   * 统一的校验与赋值入口（构造器与修改共用）
   *
   * <p>供应商字段多，构造器与 {@link #modify} 若各写一遍极易漏改。这里把「去空格 → 非空校验 →
   * 赋值」收敛到一处，后续新增字段只需改这一个方法。
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
   * @throws DomainException 代码或名称为空、结算币别为空时抛出
   */
  private void apply(
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
    String trimmedCode = StringUtils.trimToNull(code);
    String trimmedName = StringUtils.trimToNull(name);
    if (trimmedCode == null) {
      throw new DomainException("供应商代码不能为空");
    }
    if (trimmedName == null) {
      throw new DomainException("供应商名称不能为空");
    }
    if (currencyId <= 0) {
      throw new DomainException("结算币别不能为空");
    }

    this.code = trimmedCode;
    this.name = trimmedName;
    this.shortName = shortName;
    this.taxRegisterCode = StringUtils.trimToNull(taxRegisterCode);
    this.currencyId = currencyId;
    this.taxRateId = taxRateId;
    this.phone = phone;
    this.email = email;
    this.country = country;
    this.regionCode = regionCode;
    this.region = region;
    this.zip = zip;
    this.address = address;
    this.disabled = disabled;
    this.remark = remark;
  }
}
