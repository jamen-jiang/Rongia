package com.rongia.production.domain.productionreceipt;

import com.rongia.production.domain.workorder.WorkOrder;
import com.rongia.production.domain.workorder.WorkOrderRepository;
import com.rongia.shared.domain.exception.DomainException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 生产入库单领域服务
 *
 * <p>建单只该问两件事：哪张工单、入库多少。数量不填就取工单还没入库的完工数，仓库不填就用收货仓，
 * 工单号与 SPU、SKU、单位全从工单抄——单据要能独立打印，但不该让人把这些再敲一遍。
 *
 * <p>入库是跨聚合的一致性动作：入库单自己转入已入库，工单那边累加已入库数量，所以放在这里编排。
 * 库存那一半不在本模块：成品仓加数由应用层在同一步调 {@code StockDomainService.inbound} 完成。
 */
@NullMarked
public class ProductionReceiptDomainService {

  /** 生产入库单工厂 */
  private final ProductionReceiptFactory productionReceiptFactory;

  /** 生产入库单仓储 */
  private final ProductionReceiptRepository productionReceiptRepository;

  /** 工单仓储 */
  private final WorkOrderRepository workOrderRepository;

  /**
   * 构造器注入
   *
   * @param productionReceiptFactory 生产入库单工厂
   * @param productionReceiptRepository 生产入库单仓储
   * @param workOrderRepository 工单仓储
   */
  public ProductionReceiptDomainService(
      ProductionReceiptFactory productionReceiptFactory,
      ProductionReceiptRepository productionReceiptRepository,
      WorkOrderRepository workOrderRepository) {
    this.productionReceiptFactory = productionReceiptFactory;
    this.productionReceiptRepository = productionReceiptRepository;
    this.workOrderRepository = workOrderRepository;
  }

  /**
   * 创建入库单（草稿）
   *
   * <p>能入库的数量只有已完工数，而它要到末道工序完工才回填，所以未达计划数量就想入库是入不了的：
   * 报工还没做完，货就还没做出来。
   *
   * @param workOrderId 工单Id
   * @param qty 申报入库数量（可空，空即取工单还没入库的完工数）
   * @param warehouseId 入库仓库Id（可空，空即取工单上的收货仓）
   * @param remark 备注（可空）
   * @return 生产入库单聚合
   * @throws DomainException 工单不存在、没有可入库数量、或入库仓库定不下来时抛出
   */
  public ProductionReceipt create(
      long workOrderId,
      @Nullable BigDecimal qty,
      @Nullable Long warehouseId,
      @Nullable String remark) {
    WorkOrder workOrder = requireWorkOrder(workOrderId);
    BigDecimal applied = qty == null ? workOrder.unproducedQty() : qty;
    if (applied.signum() <= 0) {
      throw new DomainException(
          "工单" + workOrder.getNumber() + "没有可入库的数量，已完工" + workOrder.getCompletedQty());
    }
    // 传空串：单号交给 OrderAddedEvent 的监听器分配
    return productionReceiptFactory.create(
        workOrder.getId(),
        workOrder.getNumber(),
        workOrder.getMaterialId(),
        workOrder.getVariantId(),
        workOrder.getUnitId(),
        resolveWarehouse(workOrder, warehouseId),
        applied,
        remark,
        "");
  }

  /**
   * 修改入库单（仅草稿可改）
   *
   * <p>申报数量这里是必填的：不填就按未入库数重算的话，一个只想改仓库的动作会把小额申报顶成整单，
   * 改单的人不会预期这个结果。
   *
   * @param id 入库单Id
   * @param workOrderId 工单Id
   * @param qty 申报入库数量
   * @param warehouseId 入库仓库Id（可空，空即取工单上的收货仓）
   * @param remark 备注（可空）
   * @return 生产入库单聚合
   * @throws DomainException 入库单或工单不存在、没有可入库数量、或入库仓库定不下来时抛出
   */
  public ProductionReceipt modify(
      long id,
      long workOrderId,
      BigDecimal qty,
      @Nullable Long warehouseId,
      @Nullable String remark) {
    ProductionReceipt model = getExist(id);
    WorkOrder workOrder = requireWorkOrder(workOrderId);
    model.modify(
        workOrder.getId(),
        workOrder.getNumber(),
        workOrder.getMaterialId(),
        workOrder.getVariantId(),
        workOrder.getUnitId(),
        resolveWarehouse(workOrder, warehouseId),
        qty,
        remark);
    return model;
  }

  /**
   * 入库：实收数量落定、计入工单已入库，单据转入已入库
   *
   * <p>审核与入库合成一步。先让工单侧校验通过再改单据状态——反过来的话，工单侧一抛异常就会留下一张「已入库但
   * 没计入工单」的入库单。上限由工单说了算：累计入库不许超过已完工数量。
   *
   * <p>货进成品仓由应用层在同一事务里调库存服务完成，本方法只管单据与工单这一半。
   *
   * @param id 入库单Id
   * @param receivedQty 实收数量（可空，空即照批申报数量；短装时给实际点到的数）
   * @param time 入库时间
   * @return 生产入库单聚合
   * @throws DomainException 入库单或工单不存在、已入库过、或入库超过已完工数量时抛出
   */
  public ProductionReceipt post(
      long id, @Nullable BigDecimal receivedQty, LocalDateTime time) {
    ProductionReceipt model = getExist(id);
    WorkOrder workOrder = requireWorkOrder(model.getWorkOrderId());
    model.approve(receivedQty);
    model.post(time);
    workOrder.receiveOutput(model.getReceivedQty());
    return model;
  }

  /**
   * 红冲：把红字库存单冲掉的量从工单已入库减回去，单据停在已入库
   *
   * <p>只走已入库的单子。红字单开在库存那边，本方法只补工单这一半。单据不翻状态——货确实进过仓，是库存那边
   * 倒回去的；「被冲过没有」去库存那边查指着它的红字单。
   *
   * <p>可以分次冲，每次只传这次冲掉的量；累计上限由库存那边管，这里只管单次不超本单实收数。
   *
   * @param id 入库单Id
   * @param qty 本次冲回数量（正数）
   * @return 生产入库单聚合
   * @throws DomainException 入库单或工单不存在、单据不是已入库状态、或数量非法时抛出
   */
  public ProductionReceipt reverse(long id, BigDecimal qty) {
    ProductionReceipt model = getExist(id);
    if (!model.isPosted()) {
      throw new DomainException("入库单" + model.getNumber() + "还没入库，删掉重开就行，不用红冲");
    }
    if (qty.signum() <= 0) {
      throw new DomainException("冲回数量必须大于0");
    }
    if (qty.compareTo(model.getReceivedQty()) > 0) {
      throw new DomainException(
          "冲回数量" + qty + "不能大于本单实收数量" + model.getReceivedQty());
    }
    WorkOrder workOrder = requireWorkOrder(model.getWorkOrderId());
    workOrder.unreceiveOutput(qty);
    return model;
  }

  // ==================== 私有 ====================

  /**
   * 取入库仓库：调用方没指定就回落到工单上写的默认收货仓
   *
   * @param workOrder 工单聚合
   * @param warehouseId 调用方给的仓库Id（可空）
   * @return 入库仓库Id
   * @throws DomainException 调用方与工单都没给仓库时抛出
   */
  private Long resolveWarehouse(WorkOrder workOrder, @Nullable Long warehouseId) {
    Long resolved = warehouseId == null ? workOrder.getWarehouseId() : warehouseId;
    if (resolved == null) {
      throw new DomainException("工单" + workOrder.getNumber() + "没有默认仓库，入库仓库不能为空");
    }
    return resolved;
  }

  /**
   * 取入库单，不存在则抛异常
   *
   * @param id 入库单Id
   * @return 生产入库单聚合
   * @throws DomainException 入库单不存在时抛出
   */
  private ProductionReceipt getExist(long id) {
    ProductionReceipt model = productionReceiptRepository.get(id);
    if (model == null) {
      throw new DomainException("入库单" + id + "不存在!");
    }
    return model;
  }

  /**
   * 取工单，不存在则抛异常
   *
   * @param workOrderId 工单Id
   * @return 工单聚合
   * @throws DomainException 工单不存在时抛出
   */
  private WorkOrder requireWorkOrder(long workOrderId) {
    WorkOrder workOrder = workOrderRepository.get(workOrderId);
    if (workOrder == null) {
      throw new DomainException("工单" + workOrderId + "不存在!");
    }
    return workOrder;
  }
}
