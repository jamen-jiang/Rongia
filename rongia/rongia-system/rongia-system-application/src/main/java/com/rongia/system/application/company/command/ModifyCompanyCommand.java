package com.rongia.system.application.company.command;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 修改公司命令 */
@NullMarked
@Getter
@Schema(description = "修改公司命令")
public class ModifyCompanyCommand extends AddCompanyCommand {

  /** 公司Id */
  @Schema(description = "公司Id", requiredMode = Schema.RequiredMode.REQUIRED)
  private final Long id;

  /**
   * 构造器
   *
   * @param id 公司Id
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
   */
  public ModifyCompanyCommand(
      long id,
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
      Long currencyId,
      boolean disabled,
      @Nullable Integer sequence,
      @Nullable String remark) {
    super(
        code,
        name,
        shortName,
        parentId,
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
    this.id = id;
  }
}
