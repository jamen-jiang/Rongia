package com.rongia.inventory.domain.stockrecord;

import com.rongia.inventory.domain.warehouse.Warehouse;
import com.rongia.inventory.domain.warehouse.WarehouseRepository;
import com.rongia.shared.domain.exception.DomainException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 库存记录领域服务
 *
 * <p>开单只认来源单据：一张库存记录必须说得清是哪张业务单让它动的，仓库不会自己动账。红冲是唯一能凭空开出一张
 * 单据的动作，而它也只能照着已过账的原单开——挑原单上的行，照它的位置，数量取负，方向照抄原单，冲多少由开单
 * 的人定。
 *
 * <p>库存加减不在这里：过账这一步按行调 {@code StockDomainService} 入库或出库，由应用层编排，本模块只保证
 * 单据自己那半落定。红字单也一样，开出来先是草稿：过不过得去账取决于仓里还有没有货，过不去就让它停在草稿挂着，
 * 等人先去处理下游那张单。
 *
 * <p>库位填不填不由开单的人说了算：仓库上勾了「启用库位管理」，行就必须带库位；没勾就不许带。这一条按
 * 金蝶的口径校验在开单这一步——仓里有没有货架是仓自己的事，同一个仓不能在这张单上必填、在那张单上可空。
 * 红字单不再走一遍这个校验：它的位置是照抄已过账原单的，原单当年能过，现在就得能冲，否则仓后来改了开关
 * 就把历史账冲不掉了（金蝶对这种情况的办法是禁止改启用的仓库、新建仓调拨过去，那是应用层的约束）。
 */
@NullMarked
public class StockRecordDomainService {

  /** 库存记录仓储 */
  private final StockRecordRepository stockRecordRepository;

  /** 仓库仓储：库位填法要按仓库上的「启用库位管理」校验 */
  private final WarehouseRepository warehouseRepository;

  /**
   * 构造器注入
   *
   * @param stockRecordRepository 库存记录仓储
   * @param warehouseRepository 仓库仓储
   */
  public StockRecordDomainService(
      StockRecordRepository stockRecordRepository, WarehouseRepository warehouseRepository) {
    this.stockRecordRepository = stockRecordRepository;
    this.warehouseRepository = warehouseRepository;
  }

  /**
   * 按来源单据开一张库存记录（草稿）
   *
   * <p>不接单号入参：这张单永远由系统按来源业务单生成，不存在从别处导进来一张带号的库存记录的情况。
   *
   * @param sourceType 来源单据类型编码
   * @param sourceId 来源单据Id
   * @param sourceNumber 来源单据号（快照）
   * @param direction 出入方向编码（整张单一个方向）
   * @param operatorId 经办人Id（可空）
   * @param remark 备注（可空）
   * @param lines 库存记录行数据
   * @return 库存记录聚合
   */
  public StockRecord create(
      String sourceType,
      long sourceId,
      String sourceNumber,
      String direction,
      @Nullable Long operatorId,
      @Nullable String remark,
      List<StockRecordLineData> lines) {
    // 传空串：单号交给 OrderAddedEvent 的监听器分配
    StockRecord model =
        new StockRecord(
            sourceType, sourceId, sourceNumber, direction, operatorId, remark, "", null);
    Map<Long, Warehouse> warehouses = new HashMap<>();
    for (StockRecordLineData data : lines) {
      model.addLine(
          data.materialId(),
          data.variantId(),
          data.qty(),
          data.warehouseId(),
          data.stockStatus(),
          data.remark());
    }
    return model;
  }

  /**
   * 过账：单据转入已过账
   *
   * <p>按行加减库存由应用层在同一个事务里完成，本方法只把单据落定。
   *
   * @param id 库存记录Id
   * @param time 过账时间
   * @return 库存记录聚合
   * @throws DomainException 单据不存在、已过账或已作废、或单内没有行时抛出
   */
  public StockRecord post(long id, LocalDateTime time) {
    StockRecord model = getExist(id);
    model.post(time);
    return model;
  }

  /**
   * 红冲：照已过账的原单开一张红字单，只冲指定的几行几量
   *
   * <p>原单不动：它当初真的动过库存，改它等于把这笔账抹掉。红字单挑原单上的行、照它的位置、数量取负、方向照抄
   * 原单、指回原单。一次只冲一部分也行——同一张原单能开好几张红字单，今天冲 20、过两天再冲 10。
   *
   * <p>护栏只有一条：同一位置累计冲掉的不超过原单当初记的量。算「已冲多少」时，已经开出来的红字单都算数
   * （草稿也算、作废的不算）：草稿虽然还没搬货，可它已经把这部分额度占住了；不占的话两张草稿各冲满额，过账时
   * 谁也拦不住——仓里别的批次有货，第二张会去偷别人的账。
   *
   * <p>红字单本身不能再红冲：它当初就是往回冲的，要纠正它得另开一张正向单。
   *
   * @param id 被冲的原单Id
   * @param operatorId 经办人Id（可空）
   * @param remark 备注（可空，写冲销原因）
   * @param lines 要冲掉的行与量（数量填正数）
   * @return 红字单（草稿，由调用方保存与过账）
   * @throws DomainException 原单不存在、还没过账、本身是红字单、没给行、行对不上原单、或累计冲超原量时抛出
   */
  public StockRecord reverse(
      long id,
      @Nullable Long operatorId,
      @Nullable String remark,
      List<StockRecordLineData> lines) {
    StockRecord origin = getExist(id);
    if (!origin.isPosted()) {
      throw new DomainException("库存记录" + origin.getNumber() + "还没过账，作废就行，不用红冲");
    }
    if (origin.isReversal()) {
      throw new DomainException("库存记录" + origin.getNumber() + "本身就是红字单，不能再红冲");
    }
    if (lines.isEmpty()) {
      throw new DomainException("红字单得有行：说清冲原单的哪几行、各冲多少");
    }
    List<StockRecord> reversals =
        stockRecordRepository.getListByReversalOfRecordId(origin.getId());
    StockRecord model =
        new StockRecord(
            origin.getSourceType(),
            origin.getSourceId(),
            origin.getSourceNumber(),
            origin.getDirection(),
            operatorId,
            remark,
            "",
            origin.getId());
    for (StockRecordLineData data : lines) {
      if (data.qty().signum() <= 0) {
        throw new DomainException("冲多少要填正数，负号由单子自己落");
      }
      StockRecordLine originLine = getOriginLine(origin, data);
      BigDecimal remained = getRemainedQty(originLine, reversals);
      if (data.qty().compareTo(remained) > 0) {
        throw new DomainException(
            "物料" + data.variantId() + "在原单上只剩" + remained + "能冲，填了" + data.qty());
      }
      model.addLine(
          data.materialId(),
          data.variantId(),
          data.qty().negate(),
          data.warehouseId(),
          data.stockStatus(),
          data.remark());
    }
    return model;
  }

  /**
   * 作废：草稿不用了撕掉
   *
   * @param id 库存记录Id
   * @return 库存记录聚合
   * @throws DomainException 单据不存在、已过账或已作废时抛出
   */
  public StockRecord cancel(long id) {
    StockRecord model = getExist(id);
    model.cancel();
    return model;
  }

  // ==================== 私有 ====================

  /**
   * 在原单里找同位置的那一行
   *
   * @param origin 被冲的原单
   * @param data 要冲的行
   * @return 原单上的行
   * @throws DomainException 原单上没有这一行时抛出
   */
  private StockRecordLine getOriginLine(StockRecord origin, StockRecordLineData data) {
    for (StockRecordLine line : origin.getStockRecordLines()) {
      if (isSamePosition(
          line, data.variantId(), data.warehouseId(), data.stockStatus())) {
        return line;
      }
    }
    throw new DomainException(
        "原单上没有物料"
            + data.variantId()
            + "在仓库"
            + data.warehouseId()
            + "状态"
            + data.stockStatus()
            + "的这一行，冲不了");
  }

  /**
   * 算原单某一行还剩多少能冲
   *
   * @param originLine 原单上的行
   * @param reversals 该原单名下已开出来的红字单（含草稿，含已作废）
   * @return 还能冲掉的量
   */
  private BigDecimal getRemainedQty(
      StockRecordLine originLine, List<StockRecord> reversals) {
    BigDecimal reversed = BigDecimal.ZERO;
    for (StockRecord reversal : reversals) {
      if (reversal.isVoided()) {
        continue;
      }
      for (StockRecordLine line : reversal.getStockRecordLines()) {
        if (isSamePosition(line, originLine)) {
          reversed = reversed.add(line.getQty());
        }
      }
    }
    return originLine.getQty().add(reversed);
  }

  /**
   * 比位置：物料、仓库、库位、库存状态四项都对上，才算同一行
   *
   * <p>状态也算位置的一部分：同一个货架上的待检 100 与可用 100 是库存表里的两行，把它们认成同一行，
   * 红冲就会冲错档。
   *
   * @param line 待比的行
   * @param variantId 物料SKU
   * @param warehouseId 仓库Id
   * @param stockStatus 库存状态编码
   * @return true 表示同一位置
   */
  private boolean isSamePosition(
      StockRecordLine line,
      long variantId,
      long warehouseId,
      String stockStatus) {
    return Objects.equals(line.getVariantId(), variantId)
        && Objects.equals(line.getWarehouseId(), warehouseId)
        && Objects.equals(line.getStockStatus(), stockStatus);
  }

  /**
   * 比位置：另一行与本行是不是同一个位置
   *
   * @param line 待比的行
   * @param other 另一行
   * @return true 表示同一位置
   */
  private boolean isSamePosition(StockRecordLine line, StockRecordLine other) {
    return isSamePosition(
        line, other.getVariantId(), other.getWarehouseId(), other.getStockStatus());
  }

  /**
   * 取库存记录，不存在则抛异常
   *
   * @param id 库存记录Id
   * @return 库存记录聚合
   * @throws DomainException 库存记录不存在时抛出
   */
  private StockRecord getExist(long id) {
    StockRecord model = stockRecordRepository.get(id);
    if (model == null) {
      throw new DomainException("库存记录" + id + "不存在!");
    }
    return model;
  }
}
