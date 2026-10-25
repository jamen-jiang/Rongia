package com.rongia.system.domain.employeecontract;

import com.rongia.shared.domain.exception.DomainException;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 劳动合同工厂，负责创建合同 */
@NullMarked
public class EmployeeContractFactory {

  /** 合同仓储 */
  private final EmployeeContractRepository employeeContractRepository;

  /**
   * 构造器注入
   *
   * @param employeeContractRepository 合同仓储
   */
  public EmployeeContractFactory(EmployeeContractRepository employeeContractRepository) {
    this.employeeContractRepository = employeeContractRepository;
  }

  /**
   * 创建合同
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
   * @throws DomainException 合同编号已存在时抛出
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
    // 校验合同编号唯一
    if (employeeContractRepository.isExistByNumberExcludeId(number)) {
      throw new DomainException("合同编号" + number + "已存在!");
    }
    return new EmployeeContract(
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
}
