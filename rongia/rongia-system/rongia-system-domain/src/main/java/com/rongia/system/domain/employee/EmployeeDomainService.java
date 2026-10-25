package com.rongia.system.domain.employee;

import com.rongia.shared.domain.exception.DomainException;
import com.rongia.system.domain.company.CompanyRepository;
import com.rongia.system.domain.department.DepartmentRepository;
import com.rongia.system.domain.position.PositionRepository;
import com.rongia.system.domain.user.UserRepository;
import java.time.LocalDate;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 员工领域服务，负责跨聚合编排 */
@NullMarked
public class EmployeeDomainService {

  /** 员工工厂 */
  private final EmployeeFactory employeeFactory;

  /** 员工仓储 */
  private final EmployeeRepository employeeRepository;

  /** 公司仓储 */
  private final CompanyRepository companyRepository;

  /** 部门仓储 */
  private final DepartmentRepository departmentRepository;

  /** 岗位仓储 */
  private final PositionRepository postRepository;

  /** 用户仓储 */
  private final UserRepository userRepository;

  /**
   * 构造器注入
   *
   * @param employeeFactory 员工工厂
   * @param employeeRepository 员工仓储
   * @param companyRepository 公司仓储
   * @param departmentRepository 部门仓储
   * @param postRepository 岗位仓储
   * @param userRepository 用户仓储
   */
  public EmployeeDomainService(
      EmployeeFactory employeeFactory,
      EmployeeRepository employeeRepository,
      CompanyRepository companyRepository,
      DepartmentRepository departmentRepository,
      PositionRepository postRepository,
      UserRepository userRepository) {
    this.employeeFactory = employeeFactory;
    this.employeeRepository = employeeRepository;
    this.companyRepository = companyRepository;
    this.departmentRepository = departmentRepository;
    this.postRepository = postRepository;
    this.userRepository = userRepository;
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
   * @throws DomainException 引用的主数据不存在、或编号已存在时抛出
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
    validateReferences(companyId, departmentId, positionId, userId);
    return employeeFactory.create(
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
   * 导入生成：员工存在则修改，不存在则创建
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
   * @throws DomainException 引用的主数据不存在、或编号冲突时抛出
   */
  public Employee importGenerate(
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
    // 按编号查员工
    Employee model = employeeRepository.getByNumber(number);

    // 不存在则创建，存在则修改
    if (model == null) {
      model =
          create(
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
    } else {
      modify(
          model,
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
    return model;
  }

  /**
   * 修改员工（按Id查询后修改）
   *
   * @param id 员工Id
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
   * @throws DomainException 员工不存在时抛出
   */
  public Employee modify(
      long id,
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
    // 1. 查员工
    Employee model = employeeRepository.get(id);
    if (model == null) {
      throw new DomainException("员工不存在!");
    }
    // 2. 委托给使用已有聚合的重载
    modify(
        model,
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
    return model;
  }

  /**
   * 修改员工（使用已有聚合，避免重复查询）
   *
   * @param employee 员工聚合
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
   * @throws DomainException 引用的主数据不存在、或编号冲突时抛出
   */
  public void modify(
      Employee employee,
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
    // 1. 引用的主数据要存在
    validateReferences(companyId, departmentId, positionId, userId);
    // 2. 校验员工编号唯一（排除自身）
    if (employeeRepository.isExistByNumberExcludeId(number, employee.getId())) {
      throw new DomainException("员工编号" + number + "已存在!");
    }
    // 3. 修改主字段
    employee.modify(
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

  // ==================== 私有：校验 ====================

  /**
   * 校验引用的公司、部门、岗位、账号存在
   *
   * <p>银行账户不在这里校验：它属于 basedata 模块，system 不引入横向模块依赖，存在性由应用层保证。
   *
   * @param companyId 公司Id
   * @param departmentId 部门Id
   * @param positionId 岗位Id（可空，空表示未定岗）
   * @param userId 关联账号Id（可空）
   * @throws DomainException 任一引用的主数据不存在时抛出
   */
  private void validateReferences(
      long companyId, long departmentId, @Nullable Long positionId, @Nullable Long userId) {
    if (companyRepository.get(companyId) == null) {
      throw new DomainException("公司" + companyId + "不存在!");
    }
    if (departmentRepository.get(departmentId) == null) {
      throw new DomainException("部门" + departmentId + "不存在!");
    }
    if (positionId != null && postRepository.get(positionId) == null) {
      throw new DomainException("岗位" + positionId + "不存在!");
    }
    if (userId != null && userRepository.get(userId) == null) {
      throw new DomainException("账号" + userId + "不存在!");
    }
  }
}
