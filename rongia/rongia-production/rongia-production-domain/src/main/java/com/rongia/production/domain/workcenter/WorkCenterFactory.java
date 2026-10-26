package com.rongia.production.domain.workcenter;

import com.rongia.shared.domain.exception.DomainException;
import java.math.BigDecimal;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 工作中心工厂，负责创建工作中心 */
@NullMarked
public class WorkCenterFactory {

  /** 工作中心仓储 */
  private final WorkCenterRepository workCenterRepository;

  /**
   * 构造器注入
   *
   * @param workCenterRepository 工作中心仓储
   */
  public WorkCenterFactory(WorkCenterRepository workCenterRepository) {
    this.workCenterRepository = workCenterRepository;
  }

  /**
   * 创建工作中心
   *
   * @param name 名称
   * @param calendarId 日历Id
   * @param productionCapacity 并行接单数
   * @param hourlyRate 小时费率
   * @param status 状态编码
   * @param disabled 是否停用
   * @param remark 备注（可空）
   * @return 工作中心聚合
   * @throws DomainException 工作中心名称已存在时抛出
   */
  public WorkCenter create(
      String name,
      long calendarId,
      int productionCapacity,
      BigDecimal hourlyRate,
      String status,
      boolean disabled,
      @Nullable String remark) {
    // 校验工作中心名称唯一
    if (workCenterRepository.isExistByNameExcludeId(name)) {
      throw new DomainException("工作中心" + name + "已存在!");
    }
    return new WorkCenter(
        name,
        calendarId,
        productionCapacity,
        hourlyRate,
        status,
        disabled,
        remark);
  }
}
