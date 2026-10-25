package com.rongia.system.domain.employeecontract;

import com.rongia.shared.domain.exception.DomainException;
import com.rongia.system.domain.employee.EmployeeRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 劳动合同领域服务，负责跨聚合编排 */
@NullMarked
public class EmployeeContractDomainService {

  /** 合同工厂 */
  private final EmployeeContractFactory employeeContractFactory;

  /** 合同仓储 */
  private final EmployeeContractRepository employeeContractRepository;

  /** 员工仓储：合同必须挂在存在的员工上 */
  private final EmployeeRepository employeeRepository;

  /**
   * 构造器注入
   *
   * @param employeeContractFactory 合同工厂
   * @param employeeContractRepository 合同仓储
   * @param employeeRepository 员工仓储
   */
  public EmployeeContractDomainService(
      EmployeeContractFactory employeeContractFactory,
      EmployeeContractRepository employeeContractRepository,
      EmployeeRepository employeeRepository) {
    this.employeeContractFactory = employeeContractFactory;
    this.employeeContractRepository = employeeContractRepository;
    this.employeeRepository = employeeRepository;
  }

  /**
   * 创建合同
   *
   * <p>币种只校验非空（&gt;0），存在性交给应用层：币种在 basedata 模块，system 不引入横向模块依赖。
   *
   * @param type 合同期限类型（EmployeeContractTypeEnum 的 code）
   * @param employeeId 员工Id
   * @param number 合同编号
   * @param startDate 开始日期
   * @param endDate 结束日期（可空）
   * @param trialEndDate 试用期结束日期（可空）
   * @param signDate 签订日期（可空）
   * @param wage 合同薪酬
   * @param currencyId 薪酬币种Id
   * @param remark 备注（可空）
   * @return 合同聚合
   * @throws DomainException 员工不存在、或合同编号已存在时抛出
   */
  public EmployeeContract create(
      String type,
      long employeeId,
      String number,
      LocalDate startDate,
      @Nullable LocalDate endDate,
      @Nullable LocalDate trialEndDate,
      @Nullable LocalDate signDate,
      BigDecimal wage,
      long currencyId,
      @Nullable String remark) {
    requireEmployee(employeeId);
    return employeeContractFactory.create(
        type,
        employeeId,
        number,
        startDate,
        endDate,
        trialEndDate,
        signDate,
        wage,
        currencyId,
        remark);
  }

  /**
   * 导入生成：合同编号存在则修改，不存在则创建
   *
   * @param type 合同期限类型（EmployeeContractTypeEnum 的 code）
   * @param employeeId 员工Id
   * @param number 合同编号
   * @param startDate 开始日期
   * @param endDate 结束日期（可空）
   * @param trialEndDate 试用期结束日期（可空）
   * @param signDate 签订日期（可空）
   * @param wage 合同薪酬
   * @param currencyId 薪酬币种Id
   * @param remark 备注（可空）
   * @return 合同聚合
   */
  public EmployeeContract importGenerate(
      String type,
      long employeeId,
      String number,
      LocalDate startDate,
      @Nullable LocalDate endDate,
      @Nullable LocalDate trialEndDate,
      @Nullable LocalDate signDate,
      BigDecimal wage,
      long currencyId,
      @Nullable String remark) {
    // 按合同编号查合同
    EmployeeContract model = employeeContractRepository.getByNumber(number);

    // 不存在则创建，存在则修改
    if (model == null) {
      model =
          create(
              type,
              employeeId,
              number,
              startDate,
              endDate,
              trialEndDate,
              signDate,
              wage,
              currencyId,
              remark);
    } else {
      modify(
          model,
          number,
          type,
          startDate,
          endDate,
          trialEndDate,
          signDate,
          wage,
          currencyId,
          remark);
    }

    return model;
  }

  /**
   * 修改合同
   *
   * @param id 合同Id
   * @param type 合同期限类型（EmployeeContractTypeEnum 的 code）
   * @param number 合同编号
   * @param startDate 开始日期
   * @param endDate 结束日期（可空）
   * @param trialEndDate 试用期结束日期（可空）
   * @param signDate 签订日期（可空）
   * @param wage 合同薪酬
   * @param currencyId 薪酬币种Id
   * @param remark 备注（可空）
   * @return 合同聚合
   * @throws DomainException 合同不存在时抛出
   */
  public EmployeeContract modify(
      long id,
      String type,
      String number,
      LocalDate startDate,
      @Nullable LocalDate endDate,
      @Nullable LocalDate trialEndDate,
      @Nullable LocalDate signDate,
      BigDecimal wage,
      long currencyId,
      @Nullable String remark) {
    EmployeeContract model = employeeContractRepository.get(id);
    if (model == null) {
      throw new DomainException("合同" + id + "不存在!");
    }
    modify(
        model,
        number,
        type,
        startDate,
        endDate,
        trialEndDate,
        signDate,
        wage,
        currencyId,
        remark);
    return model;
  }

  /**
   * 修改合同（使用已有聚合）
   *
   * @param model 合同聚合
   * @param type 合同期限类型（EmployeeContractTypeEnum 的 code）
   * @param number 合同编号
   * @param startDate 开始日期
   * @param endDate 结束日期（可空）
   * @param trialEndDate 试用期结束日期（可空）
   * @param signDate 签订日期（可空）
   * @param wage 合同薪酬
   * @param currencyId 薪酬币种Id
   * @param remark 备注（可空）
   * @throws DomainException 合同编号重复时抛出
   */
  public void modify(
      EmployeeContract model,
      String type,
      String number,
      LocalDate startDate,
      @Nullable LocalDate endDate,
      @Nullable LocalDate trialEndDate,
      @Nullable LocalDate signDate,
      BigDecimal wage,
      long currencyId,
      @Nullable String remark) {
    // 校验合同编号唯一（排除自身）
    if (employeeContractRepository.isExistByNumberExcludeId(number, model.getId())) {
      throw new DomainException("合同编号" + number + "已存在!");
    }
    model.modify(
        number,
        type,
        startDate,
        endDate,
        trialEndDate,
        signDate,
        wage,
        currencyId,
        remark);
  }

  // ==================== 私有：校验 ====================

  /**
   * 校验员工存在
   *
   * @param employeeId 员工Id
   * @throws DomainException 员工不存在时抛出
   */
  private void requireEmployee(long employeeId) {
    if (employeeRepository.get(employeeId) == null) {
      throw new DomainException("员工" + employeeId + "不存在!");
    }
  }
}
