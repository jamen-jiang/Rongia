package com.rongia.system.domain.employee;

import com.rongia.shared.domain.aggregate.UpdateAggregateRoot;
import com.rongia.shared.domain.exception.DomainException;
import com.rongia.shared.domain.util.EnumUtils;
import jakarta.persistence.*;
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
 * 员工
 *
 * <p>员工是人事档案，跟 {@code User}（登录身份）分开——Odoo {@code hr.employee.user_id}、 ERPNext {@code
 * Employee.user_id} 都是**可空**的用户引用，因为没有系统账号的人也可以是员工。
 *
 * <p>归属：公司 + 部门 + 岗位。岗位是**单值**（员工上的一列 {@code PostId}）——金蝶的「员工任岗明细」
 * 支持一人多岗，但我们的权限链路只取一个岗位，主岗/调岗历史都没有消费者，所以 2026-10-22 把关系行 收回成员工上的一列；用户那边只留角色。
 *
 * <p>编号必须有：金蝶明确「员工姓名**允许重名**、员工编号数据中心唯一」——姓名不能当键。
 *
 * <p>字段口径：编号 ↔ ERPNext {@code employee_number}；身份证 ↔ Odoo {@code identification_id}； 用工类型 ↔ Odoo
 * {@code employee_type}（6 值）；离职原因/日期 ↔ ERPNext {@code reason_for_leaving} + Odoo {@code
 * departure_date}；工牌 ↔ Odoo {@code barcode}（Badge ID）； 银行账户 ↔ Odoo {@code
 * bank_account_id}（**单个**，指向通用 {@code BankAccount}）。
 *
 * <p>个人档案照 Odoo + ERPNext 补齐：性别/出生日期/婚姻状况 ↔ {@code gender} / {@code birthday} / {@code
 * marital}（ERPNext 的 {@code gender}、{@code date_of_birth} 是必填）；住址 ↔ Odoo 的 {@code private_street}
 * 一组（ERPNext 分现住址和户籍地址两套，我们只留一段）；紧急联系人 ↔ {@code emergency_contact} + {@code emergency_phone}；学历 ↔
 * {@code certificate} / {@code study_field} / {@code study_school}（ERPNext 用 {@code education} 子表）。
 * 护照/签证/照片/家庭背景等没有流程读，暂不建。
 *
 * <p>状态 {@code status} 只两档：在职 / 离职（{@link EmployeeStatusEnum}）。ERPNext 是四档、金蝶 ERP 更只有一个
 * 「禁用」开关，我们按事实收敛——离职＝不在公司了，必须填离职日期；非离职状态会把离职日期与原因清空， 避免「在职却挂着离职日期」。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "Employee",
    indexes = {
      @Index(name = "IX_Employee_Type", columnList = "Type"),
      @Index(name = "IX_Employee_CompanyId", columnList = "CompanyId"),
      @Index(name = "IX_Employee_DepartmentId", columnList = "DepartmentId"),
      @Index(name = "IX_Employee_PostId", columnList = "PostId"),
      @Index(name = "IX_Employee_UserId", columnList = "UserId"),
      @Index(name = "IX_Employee_Number", columnList = "Number"),
      @Index(name = "IX_Employee_Name", columnList = "Name"),
      @Index(name = "IX_Employee_Status", columnList = "Status")
      // 注意：Number 的过滤唯一索引带 WHERE IsDeleted=0，JPA 不支持，用 Flyway 创建
    })
public class Employee extends UpdateAggregateRoot {

  /** 用工类型编码，取值见 {@link EmployeeTypeEnum} */
  @Nationalized
  @Column(name = "Type", nullable = false, length = 50)
  private String type;

  /** 公司Id */
  @Column(name = "CompanyId", nullable = false)
  private Long companyId;

  /** 部门Id */
  @Column(name = "DepartmentId", nullable = false)
  private Long departmentId;

  /** 岗位Id（可空，先入职后定岗） */
  @Column(name = "PostId")
  @Nullable
  private Long positionId;

  /** 关联账号Id（可空，没有系统账号的人也可是员工） */
  @Column(name = "UserId")
  @Nullable
  private Long userId;

  /** 员工编号 */
  @Nationalized
  @Column(name = "Number", nullable = false, length = 100)
  private String number;

  /** 姓名 */
  @Nationalized
  @Column(name = "Name", nullable = false, length = 200)
  private String name;

  /** 工牌号（可空） */
  @Nationalized
  @Column(name = "BadgeNumber", length = 50)
  @Nullable
  private String badgeNumber;

  /** 身份证号（可空） */
  @Nationalized
  @Column(name = "IdentificationNumber", length = 30)
  @Nullable
  private String identificationNumber;

  /** 入职日期 */
  @Column(name = "JoinDate", nullable = false)
  private LocalDate joinDate;

  /** 出生日期（可空） */
  @Column(name = "BirthDate")
  @Nullable
  private LocalDate birthDate;

  /** 婚姻状况编码，取值见 {@link MaritalStatusEnum}（可空） */
  @Nationalized
  @Column(name = "MaritalStatus", length = 20)
  @Nullable
  private String maritalStatus;

  /** 银行账户Id（可空，指向 basedata 的通用 BankAccount，归属类型 EMPLOYEE） */
  @Column(name = "BankAccountId")
  @Nullable
  private Long bankAccountId;

  /** 手机号（可空） */
  @Nationalized
  @Column(name = "Mobile", length = 50)
  @Nullable
  private String mobile;

  /** 邮箱（可空） */
  @Nationalized
  @Column(name = "Email", length = 200)
  @Nullable
  private String email;

  /** 性别编码，取值见 {@link GenderEnum}（可空） */
  @Nationalized
  @Column(name = "Gender", length = 20)
  @Nullable
  private String gender;

  /** 国家（可空）：ISO 两位码 */
  @Nationalized
  @Column(name = "Country", length = 50)
  @Nullable
  private String country;

  /** 地区编码（可空）：区县一级的行政区划代码（如 440106），省市区三级都在这六位里 */
  @Nationalized
  @Column(name = "RegionCode", length = 100)
  @Nullable
  private String regionCode;

  /** 地区全称（可空）：如「广东省广州市天河区」 */
  @Nationalized
  @Column(name = "Region", length = 200)
  @Nullable
  private String region;

  /** 住址（可空，一段文本，不分现住址与户籍地址） */
  @Nationalized
  @Column(name = "Address", length = 500)
  @Nullable
  private String address;

  /** 紧急联系人姓名（可空） */
  @Nationalized
  @Column(name = "EmergencyContactName", length = 50)
  @Nullable
  private String emergencyContactName;

  /** 紧急联系人电话（可空） */
  @Nationalized
  @Column(name = "EmergencyContactPhone", length = 20)
  @Nullable
  private String emergencyContactPhone;

  /** 紧急联系人关系（可空） */
  @Nationalized
  @Column(name = "EmergencyContactRelation", length = 20)
  @Nullable
  private String emergencyContactRelation;

  /** 最高学历编码，取值见 {@link EducationLevelEnum}（可空） */
  @Nationalized
  @Column(name = "EducationLevel", length = 20)
  @Nullable
  private String educationLevel;

  /** 所学专业（可空） */
  @Nationalized
  @Column(name = "StudyField", length = 100)
  @Nullable
  private String studyField;

  /** 毕业学校（可空） */
  @Nationalized
  @Column(name = "StudySchool", length = 100)
  @Nullable
  private String studySchool;

  /** 离职日期（可空，填了就必须停用） */
  @Column(name = "DepartureDate")
  @Nullable
  private LocalDate departureDate;

  /** 离职原因（可空，自由文本，照 ERPNext 的 reason_for_leaving） */
  @Nationalized
  @Column(name = "DepartureReason", length = 500)
  @Nullable
  private String departureReason;

  /** 员工状态编码，取值见 {@link EmployeeStatusEnum} */
  @Nationalized
  @Column(name = "Status", nullable = false, length = 50)
  private String status;

  /** 备注（可空） */
  @Nationalized
  @Column(name = "Remark", length = 500)
  @Nullable
  private String remark;

  /**
   * 构造器
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
   */
  Employee(
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
    apply(
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

  /**
   * 修改员工
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
   */
  void modify(
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
    apply(
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

  // ==================== 私有：校验与赋值 ====================

  /**
   * 统一的校验与赋值入口（构造器与修改共用）
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
   * @throws DomainException 编号/姓名/公司/部门/入职日期/用工类型为空或非法、离职未停用时抛出
   */
  private void apply(
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
    String trimmedNumber = StringUtils.trimToNull(number);
    String trimmedName = StringUtils.trimToNull(name);
    if (trimmedNumber == null) {
      throw new DomainException("员工编号不能为空");
    }
    if (trimmedName == null) {
      throw new DomainException("员工姓名不能为空");
    }
    if (companyId <= 0) {
      throw new DomainException("员工所属公司不能为空");
    }
    if (departmentId <= 0) {
      throw new DomainException("员工所属部门不能为空");
    }
    if (joinDate == null) {
      throw new DomainException("员工入职日期不能为空");
    }
    this.type = EnumUtils.getCode(EmployeeTypeEnum.class, type, "员工" + trimmedNumber + "的用工类型");
    if (gender != null) {
      this.gender = EnumUtils.getCode(GenderEnum.class, gender, "员工" + trimmedNumber + "的性别");
    }
    if (maritalStatus != null) {
      this.maritalStatus =
          EnumUtils.getCode(MaritalStatusEnum.class, maritalStatus, "员工" + trimmedNumber + "的婚姻状况");
    }
    if (educationLevel != null) {
      this.educationLevel =
          EnumUtils.getCode(EducationLevelEnum.class, educationLevel, "员工" + trimmedNumber + "的学历");
    }
    if (positionId != null && positionId <= 0) {
      throw new DomainException("员工岗位不能为空");
    }
    if (userId != null && userId <= 0) {
      throw new DomainException("员工关联账号不能为空");
    }
    if (bankAccountId != null && bankAccountId <= 0) {
      throw new DomainException("员工银行账户不能为空");
    }
    this.status = EnumUtils.getCode(EmployeeStatusEnum.class, status, "员工" + trimmedNumber + "的状态");
    EmployeeStatusEnum statusEnum = EnumUtils.fromCode(EmployeeStatusEnum.class, this.status);
    boolean left = statusEnum == EmployeeStatusEnum.LEFT;
    if (left && departureDate == null) {
      throw new DomainException("员工" + trimmedNumber + "离职时必须填离职日期");
    }

    this.companyId = companyId;
    this.departmentId = departmentId;
    this.positionId = positionId;
    this.userId = userId;
    this.number = trimmedNumber;
    this.name = trimmedName;
    this.badgeNumber = StringUtils.trimToNull(badgeNumber);
    this.identificationNumber = StringUtils.trimToNull(identificationNumber);
    this.joinDate = joinDate;
    this.birthDate = birthDate;
    this.bankAccountId = bankAccountId;
    this.mobile = StringUtils.trimToNull(mobile);
    this.email = StringUtils.trimToNull(email);
    this.address = StringUtils.trimToNull(address);
    this.emergencyContactName = StringUtils.trimToNull(emergencyContactName);
    this.emergencyContactPhone = StringUtils.trimToNull(emergencyContactPhone);
    this.emergencyContactRelation = StringUtils.trimToNull(emergencyContactRelation);
    this.studyField = StringUtils.trimToNull(studyField);
    this.studySchool = StringUtils.trimToNull(studySchool);
    // 不是离职就清掉离职日期与原因，避免「在职却挂着离职日期」
    this.departureDate = left ? departureDate : null;
    this.departureReason = left ? StringUtils.trimToNull(departureReason) : null;
    this.remark = remark;
  }
}
