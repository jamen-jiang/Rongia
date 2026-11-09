package com.rongia.stock.domain.stock;

import com.rongia.stock.domain.warehouse.Warehouse;
import com.rongia.stock.domain.warehouse.WarehouseRepository;
import com.rongia.kernel.domain.exception.DomainException;
import java.math.BigDecimal;
import org.jspecify.annotations.NullMarked;

/** 库存领域服务，负责按键定位库存行并驱动数量变动 */
@NullMarked
public class StockDomainService {

  /** 库存仓储 */
  private final StockRepository stockRepository;

  /** 仓库仓储：入库建行前校验目标节点是存货点 */
  private final WarehouseRepository warehouseRepository;

  /**
   * 构造器注入
   *
   * @param stockRepository 库存仓储
   * @param warehouseRepository 仓库仓储
   */
  public StockDomainService(
      StockRepository stockRepository, WarehouseRepository warehouseRepository) {
    this.stockRepository = stockRepository;
    this.warehouseRepository = warehouseRepository;
  }

  /**
   * 入库：没有库存行就先建一行零数量，再加在库数（采购收货、完工入库、盘盈）
   *
   * <p>只有入库会建行：一键一行没有唯一索引兜底（SQL Server 唯一索引把 NULL 当值），建行前先查一次，
   * 让「先全量校验、再一次性装配」的规矩在库存这边同样成立。出库不建行，没货就是没货，直接报错。
   *
   * <p>建行前还校验目标节点：仓库树上的分组节点只归堆不存货，库存行只挂叶子（存货点）——
   * 一行库存挂到「原料仓」这种伞上，账就再也说不清是哪个货架上的了。
   *
   * @param materialId SKU所属SPU（只在新建行时写入，按系列汇总用）
   * @param variantId SKU
   * @param warehouseId 存货点Id（仓库叶子节点）
   * @param qty 入库数量
   * @return 库存行（新建的为未落库实体，由调用方保存）
   */
  public Stock inbound(long materialId, long variantId, long warehouseId, BigDecimal qty) {
    requireLeaf(warehouseId);
    Stock model = stockRepository.getByKey(variantId, warehouseId);
    if (model == null) {
      model = new Stock(materialId, variantId, warehouseId);
    }
    model.inbound(qty);
    return model;
  }

  /**
   * 出库：扣在库数（领料、销售出库、盘亏）
   *
   * <p>上限是可用量（在库 − 已答应别人的量），所以要领自己那张单批下来的货，**先调 {@link #unreserve}
   * 把这笔预留退掉再出库**；这里不顺手核销，因为库存行只有一个预留合计，不知道是谁占的，自动扣就会扣到
   * 别人头上。
   *
   * <p>在库不够就当场抛错，暂不放行负库存；以后要按仓库开关控制负库存，在库存行上沿 {@code parentPath}
   * 反查上级分组节点再判。
   *
   * @param variantId SKU
   * @param warehouseId 存货点Id（仓库叶子节点）
   * @param qty 出库数量
   * @return 库存行
   * @throws DomainException 该键没有库存行，或在库与可用不足时抛出
   */
  public Stock outbound(long variantId, long warehouseId, BigDecimal qty) {
    Stock model = getExist(variantId, warehouseId);
    model.outbound(qty);
    return model;
  }

  /**
   * 预留：把可用量占住（领料单批准、出库单分配时由应用层调）
   *
   * @param variantId SKU
   * @param warehouseId 存货点Id（仓库叶子节点）
   * @param qty 预留数量
   * @return 库存行
   * @throws DomainException 该键没有库存行或可用量不足时抛出
   */
  public Stock reserve(long variantId, long warehouseId, BigDecimal qty) {
    Stock model = getExist(variantId, warehouseId);
    model.reserve(qty);
    return model;
  }

  /**
   * 取消预留：把先前答应的量放回可用量（发料过账、单据作废、批准量调小时由应用层调）
   *
   * @param variantId SKU
   * @param warehouseId 存货点Id（仓库叶子节点）
   * @param qty 取消预留数量
   * @return 库存行
   * @throws DomainException 该键没有库存行或超过已预留时抛出
   */
  public Stock unreserve(long variantId, long warehouseId, BigDecimal qty) {
    Stock model = getExist(variantId, warehouseId);
    model.unreserve(qty);
    return model;
  }

  // ==================== 私有 ====================

  /**
   * 校验目标节点存在且是存货点
   *
   * @param warehouseId 仓库节点Id
   * @throws DomainException 节点不存在、或是分组节点时抛出
   */
  private void requireLeaf(long warehouseId) {
    Warehouse node = warehouseRepository.get(warehouseId);
    if (node == null) {
      throw new DomainException("仓库" + warehouseId + "不存在!");
    }
    if (!node.getIsLeaf()) {
      throw new DomainException(
          "仓库" + node.getName() + "是分组节点，不能直接存货，请挂到具体存货点");
    }
  }

  /**
   * 按键取库存行，不存在则抛异常
   *
   * @param variantId SKU
   * @param warehouseId 存货点Id（仓库叶子节点）
   * @return 库存行
   * @throws DomainException 该键还没有库存行时抛出
   */
  private Stock getExist(long variantId, long warehouseId) {
    Stock model = stockRepository.getByKey(variantId, warehouseId);
    if (model == null) {
      throw new DomainException("SKU" + variantId + "在仓库" + warehouseId + "没有库存行!");
    }
    return model;
  }
}
