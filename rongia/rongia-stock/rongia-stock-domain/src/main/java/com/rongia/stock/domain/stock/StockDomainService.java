package com.rongia.stock.domain.stock;

import com.rongia.shared.domain.exception.DomainException;
import java.math.BigDecimal;
import org.jspecify.annotations.NullMarked;

/** 库存领域服务，负责按键定位库存行并驱动数量变动 */
@NullMarked
public class StockDomainService {

  /** 库存仓储 */
  private final StockRepository stockRepository;

  /**
   * 构造器注入
   *
   * @param stockRepository 库存仓储
   */
  public StockDomainService(StockRepository stockRepository) {
    this.stockRepository = stockRepository;
  }

  /**
   * 入库：没有库存行就先建一行零数量，再加在库数（采购收货、完工入库、盘盈）
   *
   * <p>只有入库会建行：一键一行没有唯一索引兜底（SQL Server 唯一索引把 NULL 当值），建行前先查一次，
   * 让「先全量校验、再一次性装配」的规矩在库存这边同样成立。出库不建行，没货就是没货，直接报错。
   *
   * @param materialId SKU所属SPU（只在新建行时写入，按系列汇总用）
   * @param variantId SKU
   * @param locationId 库位Id
   * @param qty 入库数量
   * @return 库存行（新建的为未落库实体，由调用方保存）
   */
  public Stock inbound(long materialId, long variantId, long locationId, BigDecimal qty) {
    Stock model = stockRepository.getByKey(variantId, locationId);
    if (model == null) {
      model = new Stock(materialId, variantId, locationId);
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
   * <p>在库不够就当场抛错，暂不放行负库存；以后要按仓库开关控制负库存，在库存行上按库位反查仓库再判。
   *
   * @param variantId SKU
   * @param locationId 库位Id
   * @param qty 出库数量
   * @return 库存行
   * @throws DomainException 该键没有库存行，或在库与可用不足时抛出
   */
  public Stock outbound(long variantId, long locationId, BigDecimal qty) {
    Stock model = getExist(variantId, locationId);
    model.outbound(qty);
    return model;
  }

  /**
   * 预留：把可用量占住（领料单批准、出库单分配时由应用层调）
   *
   * @param variantId SKU
   * @param locationId 库位Id
   * @param qty 预留数量
   * @return 库存行
   * @throws DomainException 该键没有库存行或可用量不足时抛出
   */
  public Stock reserve(long variantId, long locationId, BigDecimal qty) {
    Stock model = getExist(variantId, locationId);
    model.reserve(qty);
    return model;
  }

  /**
   * 取消预留：把先前答应的量放回可用量（发料过账、单据作废、批准量调小时由应用层调）
   *
   * @param variantId SKU
   * @param locationId 库位Id
   * @param qty 取消预留数量
   * @return 库存行
   * @throws DomainException 该键没有库存行或超过已预留时抛出
   */
  public Stock unreserve(long variantId, long locationId, BigDecimal qty) {
    Stock model = getExist(variantId, locationId);
    model.unreserve(qty);
    return model;
  }

  // ==================== 私有 ====================

  /**
   * 按键取库存行，不存在则抛异常
   *
   * @param variantId SKU
   * @param locationId 库位Id
   * @return 库存行
   * @throws DomainException 该键还没有库存行时抛出
   */
  private Stock getExist(long variantId, long locationId) {
    Stock model = stockRepository.getByKey(variantId, locationId);
    if (model == null) {
      throw new DomainException("SKU" + variantId + "在库位" + locationId + "没有库存行!");
    }
    return model;
  }
}
