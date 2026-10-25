package com.rongia.inventory.domain.statuschange;

import com.rongia.inventory.domain.stock.Stock;
import com.rongia.inventory.domain.stock.StockDomainService;
import com.rongia.inventory.domain.warehouse.Warehouse;
import com.rongia.inventory.domain.warehouse.WarehouseRepository;
import com.rongia.shared.domain.exception.DomainException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 库存状态转换单领域服务
 *
 * <p>开单只把「哪一格、多少、从什么状态到什么状态」说清；过账才真动账：逐行调
 * {@link StockDomainService#move} 把「从状态那行减、到状态那行加」成对做完，再把这张单落定。
 * 这里不学 {@code StockRecord} 把加减留给应用层——库存记录那一半牵动外部业务单（领料单核销、工单完工），
 * 编排不归库存域；转换单的两半是同一笔账，只走一半这批数量就从账上消失了。
 *
 * <p>过账把动过的库存行原样交出去，由调用方在同一个事务里保存：状态挪完而账不落库等于白挪。
 *
 * <p>库位填不填照旧问仓库上那个「启用库位管理」，与开出入库单共用同一条规矩。
 */
@NullMarked
public class StockStatusChangeDomainService {

  /** 转换单仓储 */
  private final StockStatusChangeRepository stockStatusChangeRepository;

  /** 库存领域服务：真正挪账的那一步 */
  private final StockDomainService stockDomainService;

  /** 仓库仓储：库位填法要按仓库上的开关校验 */
  private final WarehouseRepository warehouseRepository;

  /**
   * 构造器注入
   *
   * @param stockStatusChangeRepository 转换单仓储
   * @param stockDomainService 库存领域服务
   * @param warehouseRepository 仓库仓储
   */
  public StockStatusChangeDomainService(
      StockStatusChangeRepository stockStatusChangeRepository,
      StockDomainService stockDomainService,
      WarehouseRepository warehouseRepository) {
    this.stockStatusChangeRepository = stockStatusChangeRepository;
    this.stockDomainService = stockDomainService;
    this.warehouseRepository = warehouseRepository;
  }

  /**
   * 开一张状态转换单（草稿）
   *
   * @param sourceType 来源单据类型编码（可空，检验单还没建时手工开就留空）
   * @param sourceId 来源单据Id（可空）
   * @param sourceNumber 来源单据号（可空）
   * @param operatorId 经办人Id（可空）
   * @param remark 备注（可空，写判定结论）
   * @param lines 转换单行数据
   * @return 转换单聚合
   * @throws DomainException 仓库不存在，或某行的库位填法与仓库的开关不符时抛出
   */
  public StockStatusChange create(
      @Nullable String sourceType,
      @Nullable Long sourceId,
      @Nullable String sourceNumber,
      @Nullable Long operatorId,
      @Nullable String remark,
      List<StockStatusChangeLineData> lines) {
    // 传空串：单号交给 OrderAddedEvent 的监听器分配
    StockStatusChange model =
        new StockStatusChange(sourceType, sourceId, sourceNumber, operatorId, remark, "");
    Map<Long, Warehouse> warehouses = new HashMap<>();
    for (StockStatusChangeLineData data : lines) {
      model.addLine(
          data.materialId(),
          data.variantId(),
          data.warehouseId(),
          data.fromStockStatus(),
          data.toStockStatus(),
          data.qty(),
          data.remark());
    }
    return model;
  }

  /**
   * 过账：逐行把数量从转出状态挪到转入状态，再把单据落定
   *
   * @param id 转换单Id
   * @param time 过账时间
   * @return 动过的库存行（每行挪动给出转出、转入两行），由调用方在同一事务里保存
   * @throws DomainException 转换单不存在、非草稿、没有行，或某一行挪不动时抛出
   */
  public List<Stock> post(long id, LocalDateTime time) {
    StockStatusChange model = getExist(id);
    List<Stock> moved = new ArrayList<>();
    for (StockStatusChangeLine line : model.getStockStatusChangeLines()) {
      moved.addAll(
          stockDomainService.move(
              line.getMaterialId(),
              line.getVariantId(),
              line.getWarehouseId(),
              line.getFromStockStatus(),
              line.getToStockStatus(),
              line.getQty()));
    }
    model.post(time);
    return moved;
  }

  /**
   * 作废：草稿不用了撕掉
   *
   * @param id 转换单Id
   * @return 转换单聚合
   * @throws DomainException 转换单不存在、已过账或已作废时抛出
   */
  public StockStatusChange cancel(long id) {
    StockStatusChange model = getExist(id);
    model.cancel();
    return model;
  }

  // ==================== 私有 ====================

  /**
   * 取转换单，不存在则抛异常
   *
   * @param id 转换单Id
   * @return 转换单聚合
   * @throws DomainException 转换单不存在时抛出
   */
  private StockStatusChange getExist(long id) {
    StockStatusChange model = stockStatusChangeRepository.get(id);
    if (model == null) {
      throw new DomainException("转换单" + id + "不存在!");
    }
    return model;
  }
}
