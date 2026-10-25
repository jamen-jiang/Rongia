package com.rongia.production.domain.workcenter;

import com.rongia.shared.domain.exception.DomainException;
import java.math.BigDecimal;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 工作中心领域服务，负责跨聚合编排 */
@NullMarked
public class WorkCenterDomainService {

  /** 工作中心工厂 */
  private final WorkCenterFactory workCenterFactory;

  /** 工作中心仓储 */
  private final WorkCenterRepository workCenterRepository;

  /**
   * 构造器注入
   *
   * @param workCenterFactory 工作中心工厂
   * @param workCenterRepository 工作中心仓储
   */
  public WorkCenterDomainService(
      WorkCenterFactory workCenterFactory, WorkCenterRepository workCenterRepository) {
    this.workCenterFactory = workCenterFactory;
    this.workCenterRepository = workCenterRepository;
  }

  /**
   * 导入生成：工作中心存在则修改，不存在则创建
   *
   * @param type 类型编码
   * @param code 代码
   * @param name 名称
   * @param workCalendarId 工作日历Id
   * @param efficiency 效率（百分比）
   * @param hourRate 小时费率
   * @param currencyId 小时费率的币种Id
   * @param setupTime 准备时间（分钟）
   * @param cleanupTime 清理时间（分钟）
   * @param disabled 是否停用
   * @param sequence 排序
   * @param remark 备注（可空）
   * @return 工作中心聚合
   */
  public WorkCenter importGenerate(
      String type,
      String code,
      String name,
      long workCalendarId,
      BigDecimal efficiency,
      BigDecimal hourRate,
      long currencyId,
      BigDecimal setupTime,
      BigDecimal cleanupTime,
      boolean disabled,
      int sequence,
      @Nullable String remark) {
    // 按代码查工作中心
    WorkCenter model = workCenterRepository.getByCode(code);

    // 不存在则创建，存在则修改
    if (model == null) {
      model =
          workCenterFactory.create(
              type,
              code,
              name,
              workCalendarId,
              efficiency,
              hourRate,
              currencyId,
              setupTime,
              cleanupTime,
              disabled,
              sequence,
              remark);
    } else {
      modify(
          model,
          type,
          code,
          name,
          workCalendarId,
          efficiency,
          hourRate,
          currencyId,
          setupTime,
          cleanupTime,
          disabled,
          sequence,
          remark);
    }

    return model;
  }

  /**
   * 修改工作中心
   *
   * @param id 工作中心Id
   * @param type 类型编码
   * @param code 代码
   * @param name 名称
   * @param workCalendarId 工作日历Id
   * @param efficiency 效率（百分比）
   * @param hourRate 小时费率
   * @param currencyId 小时费率的币种Id
   * @param setupTime 准备时间（分钟）
   * @param cleanupTime 清理时间（分钟）
   * @param disabled 是否停用
   * @param sequence 排序
   * @param remark 备注（可空）
   * @return 工作中心聚合
   * @throws DomainException 工作中心不存在时抛出
   */
  public WorkCenter modify(
      long id,
      String type,
      String code,
      String name,
      long workCalendarId,
      BigDecimal efficiency,
      BigDecimal hourRate,
      long currencyId,
      BigDecimal setupTime,
      BigDecimal cleanupTime,
      boolean disabled,
      int sequence,
      @Nullable String remark) {
    WorkCenter model = getExist(id);
    modify(
        model,
        type,
        code,
        name,
        workCalendarId,
        efficiency,
        hourRate,
        currencyId,
        setupTime,
        cleanupTime,
        disabled,
        sequence,
        remark);
    return model;
  }

  /**
   * 修改工作中心
   *
   * @param model 工作中心
   * @param type 类型编码
   * @param code 代码
   * @param name 名称
   * @param workCalendarId 工作日历Id
   * @param efficiency 效率（百分比）
   * @param hourRate 小时费率
   * @param currencyId 小时费率的币种Id
   * @param setupTime 准备时间（分钟）
   * @param cleanupTime 清理时间（分钟）
   * @param disabled 是否停用
   * @param sequence 排序
   * @param remark 备注（可空）
   * @throws DomainException 工作中心代码与其它中心冲突时抛出
   */
  public void modify(
      WorkCenter model,
      String type,
      String code,
      String name,
      long workCalendarId,
      BigDecimal efficiency,
      BigDecimal hourRate,
      long currencyId,
      BigDecimal setupTime,
      BigDecimal cleanupTime,
      boolean disabled,
      int sequence,
      @Nullable String remark) {
    // 校验工作中心代码唯一
    if (workCenterRepository.isExistByCodeExcludeId(code, model.getId())) {
      throw new DomainException("工作中心代码" + code + "已存在!");
    }
    model.modify(
        type,
        code,
        name,
        workCalendarId,
        efficiency,
        hourRate,
        currencyId,
        setupTime,
        cleanupTime,
        disabled,
        sequence,
        remark);
  }

  /**
   * 取工作中心，不存在直接报错
   *
   * @param id 工作中心Id
   * @return 工作中心
   * @throws DomainException 工作中心不存在时抛出
   */
  private WorkCenter getExist(long id) {
    WorkCenter model = workCenterRepository.get(id);
    if (model == null) {
      throw new DomainException("工作中心" + id + "不存在!");
    }
    return model;
  }
}
