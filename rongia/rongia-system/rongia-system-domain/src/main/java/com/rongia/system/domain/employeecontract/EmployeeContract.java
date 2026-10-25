package com.rongia.system.domain.employeecontract;

import com.rongia.shared.domain.aggregate.UpdateAggregateRoot;
import com.rongia.shared.domain.exception.DomainException;
import com.rongia.shared.domain.util.EnumUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
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
 * 劳动合同
 *
 * <p>合同是独立聚合（不是员工上的几个字段）：一个员工可以有多次合同（续签、改签）。 Odoo {@code hr.contract} 就是独立模型；Frappe/ERPNext
 * 没有员工合同模型（薪资在 {@code Salary Structure Assignment}，员工上只有一个 {@code contract_end_date}）， 所以形状以 Odoo
 * 为底，再补中国劳动合同法要求的字段。
 *
 * <p>**薪酬挂在合同上**：Odoo 是 {@code hr.contract.wage}（月薪），不是员工上的字段。 ERPNext 把 {@code
 * ctc}（薪酬总额）直接挂在员工上，我们没跟——同一个人在职期间涨薪、续签 会改合同，员工上再存一份就变成两个数各说各话。
 *
 * <p>**合同类型与员工上的用工类型是两回事**：{@code EmployeeTypeEnum} （员工/工人/实习生/外包/自由职业）说的是这个人算什么身份；这里的合同类型说的是期限怎么算
 * （固定期限 / 无固定期限 / 以完成一定工作任务为期限）。没有类型时只能拿「结束日期是否为空」 猜，会把无固定期限和以完成一定工作任务为期限混成一个。
 *
 * <p>**试用期只存结束日**：开始日就是合同开始日（Odoo {@code hr.contract.trial_date_end} 也只存
 * 结束日），试用期包含在合同期限内，所以必须落在开始日期与结束日期之间。
 *
 * <p>**不存合同状态**：未生效 / 履行中 / 已到期由签订日期与起止日期推出来；员工离职导致的终止由 员工上的离职日期表达。Odoo 的 {@code state}
 * 是审批流状态，我们没有那条流。
 *
 * <p>**不存部门/岗位快照**：签约时在哪个岗位由员工上的 {@code PostId} 表达；两处各存一份 会出现「合同说 A 岗、员工说 B 岗」。Odoo 在合同上存 {@code
 * department_id}/{@code job_id}， 是因为它要让每份合同独立表达当时的任职；我们目前没有按合同追溯岗位的需求。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "EmployeeContract",
    indexes = {
      @Index(name = "IX_EmployeeContract_Type", columnList = "Type"),
      @Index(name = "IX_EmployeeContract_EmployeeId", columnList = "EmployeeId"),
      @Index(name = "IX_EmployeeContract_Number", columnList = "Number")
      // 注意：Number 的过滤唯一索引带 WHERE IsDeleted=0，JPA 不支持，用 Flyway 创建
    })
public class EmployeeContract extends UpdateAggregateRoot {

  /** 合同期限类型编码，取值见 {@link EmployeeContractTypeEnum} */
  @Nationalized
  @Column(name = "Type", nullable = false, length = 50)
  private String type;

  /** 员工Id */
  @Column(name = "EmployeeId", nullable = false)
  private Long employeeId;

  /** 合同编号 */
  @Nationalized
  @Column(name = "Number", nullable = false, length = 100)
  private String number;

  /** 开始日期 */
  @Column(name = "StartDate", nullable = false)
  private LocalDate startDate;

  /** 结束日期（可空：无固定期限必须为空，以完成一定工作任务为期限可空） */
  @Column(name = "EndDate")
  @Nullable
  private LocalDate endDate;

  /** 试用期结束日期（可空，空表示不约定试用期） */
  @Column(name = "TrialEndDate")
  @Nullable
  private LocalDate trialEndDate;

  /** 签订日期（可空，空表示尚未签订） */
  @Column(name = "SignDate")
  @Nullable
  private LocalDate signDate;

  /** 合同薪酬 */
  @Column(name = "Wage", nullable = false, precision = 18, scale = 2)
  private BigDecimal wage;

  /** 薪酬币种Id */
  @Column(name = "CurrencyId", nullable = false)
  private Long currencyId;

  /** 备注（可空） */
  @Nationalized
  @Column(name = "Remark", length = 500)
  @Nullable
  private String remark;

  /**
   * 构造器
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
   */
  EmployeeContract(
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
    apply(
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
   * 修改合同
   *
   * <p>所属员工是这一行的身份，不提供修改：换人就当新合同建。
   *
   * @param type 合同期限类型（EmployeeContractTypeEnum 的 code）
   * @param number 合同编号
   * @param startDate 开始日期
   * @param endDate 结束日期（可空）
   * @param trialEndDate 试用期结束日期（可空）
   * @param signDate 签订日期（可空）
   * @param wage 合同薪酬
   * @param currencyId 薪酬币种Id
   * @param remark 备注（可空）
   */
  void modify(
      String type,
      String number,
      LocalDate startDate,
      @Nullable LocalDate endDate,
      @Nullable LocalDate trialEndDate,
      @Nullable LocalDate signDate,
      BigDecimal wage,
      long currencyId,
      @Nullable String remark) {
    apply(
        type,
        this.employeeId,
        number,
        startDate,
        endDate,
        trialEndDate,
        signDate,
        wage,
        currencyId,
        remark);
  }

  // ==================== 私有：校验与赋值 ====================

  /**
   * 统一的校验与赋值入口（构造器与修改共用）
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
   * @throws DomainException 必填项为空、合同类型有误、日期不符合期限类型时抛出
   */
  private void apply(
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
    String trimmedNumber = StringUtils.trimToNull(number);
    if (employeeId <= 0) {
      throw new DomainException("合同必须属于某个员工");
    }
    if (trimmedNumber == null) {
      throw new DomainException("合同编号不能为空");
    }
    EmployeeContractTypeEnum typeEnum = EnumUtils.fromCode(EmployeeContractTypeEnum.class, type);
    if (typeEnum == null) {
      throw new DomainException("合同类型" + type + "有误!");
    }
    if (startDate == null) {
      throw new DomainException("合同开始日期不能为空");
    }
    if (typeEnum == EmployeeContractTypeEnum.FIXED_TERM && endDate == null) {
      throw new DomainException("固定期限合同必须填结束日期");
    }
    if (typeEnum == EmployeeContractTypeEnum.OPEN_ENDED && endDate != null) {
      throw new DomainException("无固定期限合同不能填结束日期");
    }
    if (endDate != null && endDate.isBefore(startDate)) {
      throw new DomainException("合同结束日期不能早于开始日期");
    }
    if (trialEndDate != null) {
      if (!trialEndDate.isAfter(startDate)) {
        throw new DomainException("试用期结束日期必须晚于合同开始日期");
      }
      if (endDate != null && trialEndDate.isAfter(endDate)) {
        throw new DomainException("试用期结束日期不能晚于合同结束日期");
      }
    }
    if (wage == null || wage.signum() < 0) {
      throw new DomainException("合同薪酬不能为空且不能为负数");
    }
    if (currencyId <= 0) {
      throw new DomainException("合同薪酬币种不能为空");
    }

    this.type = type;
    this.employeeId = employeeId;
    this.number = trimmedNumber;
    this.startDate = startDate;
    this.endDate = endDate;
    this.trialEndDate = trialEndDate;
    this.signDate = signDate;
    this.wage = wage;
    this.currencyId = currencyId;
    this.remark = remark;
  }
}
