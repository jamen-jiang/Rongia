package com.rongia.purchase.domain.purchaseorder;

import com.rongia.shared.domain.exception.DomainException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 采购单领域服务
 *
 * <p>只管采购单自己的事：建单带行、改行、转待收货、取消。供应商、币种、物料是不是真的存在，本模块查不到——那几个
 * 主数据在基础资料模块，采购域不依赖它，由应用层在同一个事务里先校验再调这里。同理，转待收货后开收货单也是应用层
 * 编排：采购域不认识收货单、不认识库存。
 */
@NullMarked
public class PurchaseOrderDomainService {

  /** 采购单工厂 */
  private final PurchaseOrderFactory purchaseOrderFactory;

  /** 采购单仓储 */
  private final PurchaseOrderRepository purchaseOrderRepository;

  /**
   * 构造器注入
   *
   * @param purchaseOrderFactory 采购单工厂
   * @param purchaseOrderRepository 采购单仓储
   */
  public PurchaseOrderDomainService(
      PurchaseOrderFactory purchaseOrderFactory,
      PurchaseOrderRepository purchaseOrderRepository) {
    this.purchaseOrderFactory = purchaseOrderFactory;
    this.purchaseOrderRepository = purchaseOrderRepository;
  }

  /**
   * 创建采购单（草稿，可一次带齐行）
   *
   * @param supplierId 供应商Id
   * @param supplierOrderNumber 供应商单号（可空）
   * @param orderTime 下单时间
   * @param deliveryTime 交期（可空）
   * @param remark 备注（可空）
   * @param lines 采购单行数据
   * @return 采购单聚合
   */
  public PurchaseOrder create(
      long supplierId,
      @Nullable String supplierOrderNumber,
      LocalDateTime orderTime,
      @Nullable LocalDateTime deliveryTime,
      @Nullable String remark,
      List<PurchaseOrderLineData> lines) {
    // 传空串：单号交给 OrderAddedEvent 的监听器分配
    PurchaseOrder model =
        purchaseOrderFactory.create(
            supplierId, supplierOrderNumber, orderTime, deliveryTime, remark, "");
    for (PurchaseOrderLineData data : lines) {
      model.addLine(
          data.materialId(),
          data.variantId(),
          data.supplierMaterialCode(),
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
   * @param id 采购单Id
   * @param supplierId 供应商Id
   * @param supplierOrderNumber 供应商单号（可空）
   * @param orderTime 下单时间
   * @param deliveryTime 交期（可空）
   * @param remark 备注（可空）
   * @return 采购单聚合
   * @throws DomainException 采购单不存在时抛出
   */
  public PurchaseOrder modify(
      long id,
      long supplierId,
      @Nullable String supplierOrderNumber,
      LocalDateTime orderTime,
      @Nullable LocalDateTime deliveryTime,
      @Nullable String remark) {
    PurchaseOrder model = getExist(id);
    model.modify(
        supplierId, supplierOrderNumber, orderTime, deliveryTime, remark);
    return model;
  }

  /**
   * 添加采购单行（仅草稿可加）
   *
   * @param id 采购单Id
   * @param data 采购单行数据
   * @return 采购单聚合
   * @throws DomainException 采购单不存在时抛出
   */
  public PurchaseOrder addLine(long id, PurchaseOrderLineData data) {
    PurchaseOrder model = getExist(id);
    model.addLine(
        data.materialId(),
        data.variantId(),
        data.supplierMaterialCode(),
        data.qty(),
        data.unitId(),
        data.price(),
        data.discount(),
        data.taxRateId(),
        data.taxRate());
    return model;
  }

  /**
   * 修改采购单行（仅草稿可改）
   *
   * @param id 采购单Id
   * @param purchaseOrderLineId 采购单行Id
   * @param data 采购单行数据
   * @return 采购单聚合
   * @throws DomainException 采购单或采购单行不存在时抛出
   */
  public PurchaseOrder modifyLine(long id, long purchaseOrderLineId, PurchaseOrderLineData data) {
    PurchaseOrder model = getExist(id);
    model.modifyLine(
        purchaseOrderLineId,
        data.materialId(),
        data.variantId(),
        data.supplierMaterialCode(),
        data.qty(),
        data.unitId(),
        data.price(),
        data.discount(),
        data.taxRateId(),
        data.taxRate());
    return model;
  }

  /**
   * 删除采购单行（仅草稿可删）
   *
   * @param id 采购单Id
   * @param purchaseOrderLineId 采购单行Id
   * @return 采购单聚合
   * @throws DomainException 采购单不存在时抛出
   */
  public PurchaseOrder deleteLine(long id, long purchaseOrderLineId) {
    PurchaseOrder model = getExist(id);
    model.deleteLine(purchaseOrderLineId);
    return model;
  }

  /**
   * 确认：单据算数，供应商与价格定死
   *
   * @param id 采购单Id
   * @return 采购单聚合
   * @throws DomainException 采购单不存在、或单内没有行时抛出
   */
  public PurchaseOrder confirm(long id) {
    PurchaseOrder model = getExist(id);
    model.confirm();
    return model;
  }

  /**
   * 收货：把本次到的货记到某一行的已收数量上
   *
   * <p>开收货单、把货入到哪个库位由应用层在同一个事务里编排：本方法只把数量累加到采购行的已收上，
   * 库存那半边由库存模块记账。
   *
   * @param id 采购单Id
   * @param purchaseOrderLineId 采购单行Id
   * @param qty 本次收货数量
   * @return 采购单聚合
   * @throws DomainException 采购单不存在、单据未确认/已取消/已关闭、行不存在、或累计收货超过订单量时抛出
   */
  public PurchaseOrder receive(long id, long purchaseOrderLineId, BigDecimal qty) {
    PurchaseOrder model = getExist(id);
    model.receive(purchaseOrderLineId, qty);
    return model;
  }

  /**
   * 退货：把某一行的已收数量减回去，退货数量累加上去
   *
   * <p>开采购退货出库单由应用层在同一个事务里编排：本方法只动采购行上的两个数量，
   * 库存那半边由库存模块开一张「采购退货」单记账。
   *
   * @param id 采购单Id
   * @param purchaseOrderLineId 采购单行Id
   * @param qty 本次退货数量
   * @return 采购单聚合
   * @throws DomainException 采购单不存在、单据未确认/已取消/已关闭、行不存在、或退货超过已收数量时抛出
   */
  public PurchaseOrder unreceive(long id, long purchaseOrderLineId, BigDecimal qty) {
    PurchaseOrder model = getExist(id);
    model.unreceive(purchaseOrderLineId, qty);
    return model;
  }

  /**
   * 关闭：结案，剩余额度作罢
   *
   * @param id 采购单Id
   * @return 采购单聚合
   * @throws DomainException 采购单不存在、已取消或已关闭时抛出
   */
  public PurchaseOrder close(long id) {
    PurchaseOrder model = getExist(id);
    model.close();
    return model;
  }

  /**
   * 取消采购单
   *
   * <p>已提交的单子要先由应用层把它开出去的收货单取消掉，再来调这里。
   *
   * @param id 采购单Id
   * @return 采购单聚合
   * @throws DomainException 采购单不存在、已取消或已关闭时抛出
   */
  public PurchaseOrder cancel(long id) {
    PurchaseOrder model = getExist(id);
    model.cancel();
    return model;
  }

  // ==================== 私有 ====================

  /**
   * 取采购单，不存在则抛异常
   *
   * @param id 采购单Id
   * @return 采购单聚合
   * @throws DomainException 采购单不存在时抛出
   */
  private PurchaseOrder getExist(long id) {
    PurchaseOrder model = purchaseOrderRepository.get(id);
    if (model == null) {
      throw new DomainException("采购单" + id + "不存在!");
    }
    return model;
  }
}
