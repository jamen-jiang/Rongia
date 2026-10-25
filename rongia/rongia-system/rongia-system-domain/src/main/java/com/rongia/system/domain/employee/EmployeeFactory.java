package com.rongia.system.domain.employee;

import com.rongia.shared.domain.exception.DomainException;
import java.time.LocalDate;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 员工工厂，负责创建员工 */
@NullMarked
public class EmployeeFactory {

  /** 员工仓储 */
  private final EmployeeRepository employeeRepository;

  /**
   * 构造器注入
   *
   * @param employeeRepository 员工仓储
   */
  public EmployeeFactory(EmployeeRepository employeeRepository) {
    this.employeeRepository = employeeRepository;
  }

  /**
   * 创建员工
   *
   * @param type 用工类型编码
   * @param companyId 公司Id
   * @param departmentId 部门Id
   * @param positionId 岗位Id（可空，空表示未定岗）
   * @param userId 关联账号Id（可空）
   * @param number 员工编号
   * @param name 姓名
   * @param badgeNumber 工牌号（可空）
   * @param identificationNumber 身份证号（可空）
   * @param joinDate 入职日期
   * @param birthDate 出生日期（可空）
   * @param maritalStatus 婚姻状况编码（可空）
   * @param bankAccountId 银行账户Id（可空）
   * @param mobile 手机号（可空）
   * @param email 邮箱（可空）
   * @param gender 性别编码（可空）
   * @param country 国家（可空）
   * @param regionCode 地区编码（可空）：区县一级的行政区划代码，省市区都在这六位里
   * @param region 地区全称（可空）：如「广东省广州市天河区」
   * @param address 住址（可空）
   * @param emergencyContactName 紧急联系人姓名（可空）
   * @param emergencyContactPhone 紧急联系人电话（可空）
   * @param emergencyContactRelation 紧急联系人关系（可空）
   * @param educationLevel 最高学历编码（可空）
   * @param studyField 所学专业（可空）
   * @param studySchool 毕业学校（可空）
   * @param departureDate 离职日期（可空）
   * @param departureReason 离职原因（可空）
   * @param status 员工状态编码
   * @param remark 备注（可空）
   * @return 员工聚合
   * @throws DomainException 员工编号已存在时抛出
   */
  public Employee create(
      String type,
      long companyId,
      long departmentId,
      @Nullable Long positionId,
      @Nullable Long userId,
      String number,
      String name,
      @Nullable String badgeNumber,
      @Nullable String identificationNumber,
      LocalDate joinDate,
      @Nullable LocalDate birthDate,
      @Nullable String maritalStatus,
      @Nullable Long bankAccountId,
      @Nullable String mobile,
      @Nullable String email,
      @Nullable String gender,
      @Nullable String country,
      @Nullable String regionCode,
      @Nullable String region,
      @Nullable String address,
      @Nullable String emergencyContactName,
      @Nullable String emergencyContactPhone,
      @Nullable String emergencyContactRelation,
      @Nullable String educationLevel,
      @Nullable String studyField,
      @Nullable String studySchool,
      @Nullable LocalDate departureDate,
      @Nullable String departureReason,
      String status,
      @Nullable String remark) {
    // 校验员工编号唯一
    if (employeeRepository.isExistByNumberExcludeId(number)) {
      throw new DomainException("员工编号" + number + "已存在!");
    }
    return new Employee(
        type,
        companyId,
        departmentId,
        positionId,
        userId,
        number,
        name,
        badgeNumber,
        identificationNumber,
        joinDate,
        birthDate,
        maritalStatus,
        bankAccountId,
        mobile,
        email,
        gender,
        country,
        regionCode,
        region,
        address,
        emergencyContactName,
        emergencyContactPhone,
        emergencyContactRelation,
        educationLevel,
        studyField,
        studySchool,
        departureDate,
        departureReason,
        status,
        remark);
  }
}
