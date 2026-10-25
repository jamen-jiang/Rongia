package com.rongia.sales.domain.salesorder;

import com.rongia.shared.domain.exception.DomainException;
import java.time.LocalDateTime;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 销售订单领域服务
 *
 * <p>只管销售订单自己的事：建单带行、改行、确认、取消。客户、币种、业务员是不是真的存在，本模块查不到——那几个
 * 主数据在基础资料模块，销售域不依赖它，由应用层在同一个事务里先校验再调这里。同理，确认后按行派工单也是应用层
 * 编排：销售域不认识工单。
 */
@NullMarked
public class SalesOrderDomainService {

  /** 销售订单工厂 */
  private final SalesOrderFactory salesOrderFactory;

  /** 销售订单仓储 */
  private final SalesOrderRepository salesOrderRepository;

  /**
   * 构造器注入
   *
   * @param salesOrderFactory 销售订单工厂
   * @param salesOrderRepository 销售订单仓储
   */
  public SalesOrderDomainService(
      SalesOrderFactory salesOrderFactory, SalesOrderRepository salesOrderRepository) {
    this.salesOrderFactory = salesOrderFactory;
    this.salesOrderRepository = salesOrderRepository;
  }

  /**
   * 创建销售订单（草稿，可一次带齐行）
   *
   * @param customerId 客户Id
   * @param customerOrderNumber 客户单号（可空）
   * @param orderTime 下单时间
   * @param deliveryTime 交期（可空）
   * @param remark 备注（可空）
   * @param lines 订单行数据
   * @return 销售订单聚合
   */
  public SalesOrder create(
      long customerId,
      @Nullable String customerOrderNumber,
      LocalDateTime orderTime,
      @Nullable LocalDateTime deliveryTime,
      @Nullable String remark,
      List<SalesOrderLineData> lines) {
    // 传空串：单号交给 OrderAddedEvent 的监听器分配
    SalesOrder model =
        salesOrderFactory.create(
            customerId, customerOrderNumber, orderTime, deliveryTime, remark, "");
    for (SalesOrderLineData data : lines) {
      model.addLine(
          data.sequence(),
          data.materialId(),
          data.variantId(),
          data.customerMaterialCode(),
          data.customerMaterialName(),
          data.qty(),
          data.unitId(),
          data.price(),
          data.discount(),
          data.taxRateId(),
          data.taxRate());
    }
    return model;
  }

  /**
   * 修改单据头（仅草稿可改）
   *
   * @param id 销售订单Id
   * @param customerId 客户Id
   * @param customerOrderNumber 客户单号（可空）
   * @param orderTime 下单时间
   * @param deliveryTime 交期（可空）
   * @param remark 备注（可空）
   * @return 销售订单聚合
   * @throws DomainException 销售订单不存在时抛出
   */
  public SalesOrder modify(
      long id,
      long customerId,
      @Nullable String customerOrderNumber,
      LocalDateTime orderTime,
      @Nullable LocalDateTime deliveryTime,
      @Nullable String remark) {
    SalesOrder model = getExist(id);
    model.modify(customerId, customerOrderNumber, orderTime, deliveryTime, remark);
    return model;
  }

  /**
   * 添加订单行（仅草稿可加）
   *
   * @param id 销售订单Id
   * @param data 订单行数据
   * @return 销售订单聚合
   * @throws DomainException 销售订单不存在时抛出
   */
  public SalesOrder addLine(long id, SalesOrderLineData data) {
    SalesOrder model = getExist(id);
    model.addLine(
        data.sequence(),
        data.materialId(),
        data.variantId(),
        data.customerMaterialCode(),
        data.customerMaterialName(),
        data.qty(),
        data.unitId(),
        data.price(),
        data.discount(),
        data.taxRateId(),
        data.taxRate());
    return model;
  }

  /**
   * 修改订单行（仅草稿可改）
   *
   * @param id 销售订单Id
   * @param salesOrderLineId 销售订单行Id
   * @param data 订单行数据
   * @return 销售订单聚合
   * @throws DomainException 销售订单或订单行不存在时抛出
   */
  public SalesOrder modifyLine(long id, long salesOrderLineId, SalesOrderLineData data) {
    SalesOrder model = getExist(id);
    model.modifyLine(
        salesOrderLineId,
        data.sequence(),
        data.materialId(),
        data.variantId(),
        data.customerMaterialCode(),
        data.customerMaterialName(),
        data.qty(),
        data.unitId(),
        data.price(),
        data.discount(),
        data.taxRateId(),
        data.taxRate());
    return model;
  }

  /**
   * 删除订单行（仅草稿可删）
   *
   * @param id 销售订单Id
   * @param salesOrderLineId 销售订单行Id
   * @return 销售订单聚合
   * @throws DomainException 销售订单不存在时抛出
   */
  public SalesOrder deleteLine(long id, long salesOrderLineId) {
    SalesOrder model = getExist(id);
    model.deleteLine(salesOrderLineId);
    return model;
  }

  /**
   * 转待发货：单据算数，客户与价格定死
   *
   * <p>按行派工单由应用层在同一个事务里接着做：先让本单转过去，再逐行开单——反过来先开工单，本单一旦转不过去
   * 就会留下一批无主工单。
   *
   * @param id 销售订单Id
   * @return 销售订单聚合
   * @throws DomainException 销售订单不存在、或单内没有行时抛出
   */
  public SalesOrder deliver(long id) {
    SalesOrder model = getExist(id);
    model.deliver();
    return model;
  }

  /**
   * 完成：货全发完了
   *
   * @param id 销售订单Id
   * @return 销售订单聚合
   * @throws DomainException 销售订单不存在、或非待发货状态时抛出
   */
  public SalesOrder complete(long id) {
    SalesOrder model = getExist(id);
    model.complete();
    return model;
  }

  /**
   * 关闭：结案，剩余额度作罢
   *
   * @param id 销售订单Id
   * @return 销售订单聚合
   * @throws DomainException 销售订单不存在、已取消或已关闭时抛出
   */
  public SalesOrder close(long id) {
    SalesOrder model = getExist(id);
    model.close();
    return model;
  }

  /**
   * 取消销售订单
   *
   * <p>已提交的单子要先由应用层把它派出去的工单取消掉，再来调这里。
   *
   * @param id 销售订单Id
   * @return 销售订单聚合
   * @throws DomainException 销售订单不存在、已取消或已关闭时抛出
   */
  public SalesOrder cancel(long id) {
    SalesOrder model = getExist(id);
    model.cancel();
    return model;
  }

  // ==================== 私有 ====================

  /**
   * 取销售订单，不存在则抛异常
   *
   * @param id 销售订单Id
   * @return 销售订单聚合
   * @throws DomainException 销售订单不存在时抛出
   */
  private SalesOrder getExist(long id) {
    SalesOrder model = salesOrderRepository.get(id);
    if (model == null) {
      throw new DomainException("销售订单" + id + "不存在!");
    }
    return model;
  }
}
