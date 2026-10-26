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
   * @param name 名称
   * @param calendarId 日历Id
   * @param productionCapacity 并行接单数
   * @param hourlyRate 小时费率
   * @param status 状态编码
   * @param disabled 是否停用
   * @param remark 备注（可空）
   * @return 工作中心聚合
   */
  public WorkCenter importGenerate(
      String name,
      long calendarId,
      int productionCapacity,
      BigDecimal hourlyRate,
      String status,
      boolean disabled,
      @Nullable String remark) {
    // 按名称查工作中心
    WorkCenter model = workCenterRepository.getByName(name);

    // 不存在则创建，存在则修改
    if (model == null) {
      model =
          workCenterFactory.create(
              name,
              calendarId,
              productionCapacity,
              hourlyRate,
              status,
              disabled,
              remark);
    } else {
      modify(
          model,
          name,
          calendarId,
          productionCapacity,
          hourlyRate,
          status,
          disabled,
          remark);
    }

    return model;
  }

  /**
   * 修改工作中心
   *
   * @param id 工作中心Id
   * @param name 名称
   * @param calendarId 日历Id
   * @param productionCapacity 并行接单数
   * @param hourlyRate 小时费率
   * @param status 状态编码
   * @param disabled 是否停用
   * @param remark 备注（可空）
   * @return 工作中心聚合
   * @throws DomainException 工作中心不存在时抛出
   */
  public WorkCenter modify(
      long id,
      String name,
      long calendarId,
      int productionCapacity,
      BigDecimal hourlyRate,
      String status,
      boolean disabled,
      @Nullable String remark) {
    WorkCenter model = getExist(id);
    modify(
        model,
        name,
        calendarId,
        productionCapacity,
        hourlyRate,
        status,
        disabled,
        remark);
    return model;
  }

  /**
   * 修改工作中心
   *
   * @param model 工作中心
   * @param name 名称
   * @param calendarId 日历Id
   * @param productionCapacity 并行接单数
   * @param hourlyRate 小时费率
   * @param status 状态编码
   * @param disabled 是否停用
   * @param remark 备注（可空）
   */
  public void modify(
      WorkCenter model,
      String name,
      long calendarId,
      int productionCapacity,
      BigDecimal hourlyRate,
      String status,
      boolean disabled,
      @Nullable String remark) {
    model.modify(
        name,
        calendarId,
        productionCapacity,
        hourlyRate,
        status,
        disabled,
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
