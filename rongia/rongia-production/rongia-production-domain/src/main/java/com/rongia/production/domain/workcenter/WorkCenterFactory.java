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
   * @param workCalendarId 工作日历Id
   * @param efficiency 效率（百分比）
   * @param hourRate 小时费率
   * @param currencyId 小时费率的币种Id
   * @param setupTime 准备时间（分钟）
   * @param cleanupTime 清理时间（分钟）
   * @param defaultCapacity 默认产能
   * @param sourceWarehouseId 默认发料仓库Id（可空）
   * @param destWarehouseId 默认收货仓库Id（可空）
   * @param workingState 实时状态编码
   * @param disabled 是否停用
   * @param sequence 排序
   * @param remark 备注（可空）
   * @return 工作中心聚合
   * @throws DomainException 工作中心名称已存在时抛出
   */
  public WorkCenter create(
      String name,
      long workCalendarId,
      BigDecimal efficiency,
      BigDecimal hourRate,
      long currencyId,
      BigDecimal setupTime,
      BigDecimal cleanupTime,
      int defaultCapacity,
      @Nullable Long sourceWarehouseId,
      @Nullable Long destWarehouseId,
      String workingState,
      boolean disabled,
      int sequence,
      @Nullable String remark) {
    // 校验工作中心名称唯一
    if (workCenterRepository.isExistByNameExcludeId(name)) {
      throw new DomainException("工作中心" + name + "已存在!");
    }
    return new WorkCenter(
        name,
        workCalendarId,
        efficiency,
        hourRate,
        currencyId,
        setupTime,
        cleanupTime,
        defaultCapacity,
        sourceWarehouseId,
        destWarehouseId,
        workingState,
        disabled,
        sequence,
        remark);
  }
}
