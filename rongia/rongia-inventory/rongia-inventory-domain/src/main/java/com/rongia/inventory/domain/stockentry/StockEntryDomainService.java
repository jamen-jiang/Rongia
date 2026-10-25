package com.rongia.inventory.domain.stockentry;

import com.rongia.inventory.domain.stockentrytype.StockEntryType;
import com.rongia.inventory.domain.stockentrytype.StockEntryTypeEnum;
import com.rongia.inventory.domain.stockentrytype.StockEntryTypeRepository;
import com.rongia.inventory.domain.warehouse.Warehouse;
import com.rongia.inventory.domain.warehouse.WarehouseRepository;
import com.rongia.shared.domain.exception.DomainException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 库存单据领域服务
 *
 * <p>类型不用人选：开单时按行上的仓推出编码（源仓+目标仓都填是调拨、只填源仓是领料出库、只填目标仓是收货
 * 入库），再按编码反查类型表里的标准行落到单头上——ERPNext 的 {@code make_stock_entry} 原样照搬的链路。
 * 来源单据可空——系统照业务单生成的单子带来源，车间手工开的单不填。红冲是唯一能凭空开出一张单据的动作，
 * 而它也只能照着已提交的原单开——挑原单上的行，照它的位置，数量取负，类型照抄原单，冲多少由开单的人定。
 *
 * <p>库存加减不在这里：过账这一步按行调 {@code StockDomainService} 入库或出库，由应用层编排，本模块只保证
 * 单据自己那半落定。红字单也一样，开出来先是草稿：过不过得去账取决于仓里还有没有货，过不去就让它停在草稿挂着，
 * 等人先去处理下游那张单。
 *
 * <p>库位填不填不由开单的人说了算：仓库上勾了「启用库位管理」，行就必须带库位；没勾就不许带。这一条按
 * 金蝶的口径校验在开单这一步——仓里有没有货架是仓自己的事，同一个仓不能在这张单上必填、在那张单上可空。
 * 红字单不再走一遍这个校验：它的位置是照抄已提交原单的，原单当年能过，现在就得能冲，否则仓后来改了开关
 * 就把历史账冲不掉了（金蝶对这种情况的办法是禁止改启用的仓库、新建仓调拨过去，那是应用层的约束）。
 */
@NullMarked
public class StockEntryDomainService {

  /** 库存单据仓储 */
  private final StockEntryRepository stockEntryRepository;

  /** 库存单据类型仓储：开单时按类型带出方向 */
  private final StockEntryTypeRepository stockEntryTypeRepository;

  /** 仓库仓储：库位填法要按仓库上的「启用库位管理」校验 */
  private final WarehouseRepository warehouseRepository;

  /**
   * 构造器注入
   *
   * @param stockEntryRepository 库存单据仓储
   * @param stockEntryTypeRepository 库存单据类型仓储
   * @param warehouseRepository 仓库仓储
   */
  public StockEntryDomainService(
      StockEntryRepository stockEntryRepository,
      StockEntryTypeRepository stockEntryTypeRepository,
      WarehouseRepository warehouseRepository) {
    this.stockEntryRepository = stockEntryRepository;
    this.stockEntryTypeRepository = stockEntryTypeRepository;
    this.warehouseRepository = warehouseRepository;
  }

  /**
   * 开一张库存单据（草稿）
   *
   * <p>单号不接入参：由 OrderAddedEvent 的监听器按流水号规则分配。类型不接入参：按第一行的仓推出编码，
   * 再反查类型表里的标准行；类型停用了就开不出单。手工开单想挂在自建类型名下的场景，等应用层铺到再扩。
   *
   * @param sourceType 来源单据类型编码（可空，手工开单不填）
   * @param sourceId 来源单据Id（可空）
   * @param sourceNumber 来源单据号（快照，可空）
   * @param remark 备注（可空）
   * @param lines 库存单据行数据
   * @return 库存单据聚合
   * @throws DomainException 行集为空、各行的仓推出编码不一致、标准类型行缺失或已停用、或来源只填一半时抛出
   */
  public StockEntry create(
      @Nullable String sourceType,
      @Nullable Long sourceId,
      @Nullable String sourceNumber,
      @Nullable String remark,
      List<StockEntryLineData> lines) {
    if (lines.isEmpty()) {
      throw new DomainException("库存单据得有行：说清哪个SKU从哪个仓到哪个仓");
    }
    String code = resolveCode(lines);
    StockEntryType entryType = getStandardType(code);
    // 传空串：单号交给 OrderAddedEvent 的监听器分配
    StockEntry model =
        new StockEntry(
            code,
            sourceType,
            sourceId,
            sourceNumber,
            remark,
            "",
            null);
    for (StockEntryLineData data : lines) {
      model.addLine(
          data.materialId(),
          data.variantId(),
          data.qty(),
          data.sourceWarehouseId(),
          data.targetWarehouseId(),
          data.remark());
    }
    return model;
  }

  /**
   * 提交：单据转入已提交
   *
   * <p>按行加减库存由应用层在同一个事务里完成，本方法只把单据落定。
   *
   * @param id 库存单据Id
   * @param time 提交时间
   * @return 库存单据聚合
   * @throws DomainException 单据不存在、已提交、或单内没有行时抛出
   */
  public StockEntry submit(long id, LocalDateTime time) {
    StockEntry model = getExist(id);
    model.submit(time);
    return model;
  }

  /**
   * 红冲：照已提交的原单开一张红字单，只冲指定的几行几量
   *
   * <p>原单不动：它当初真的动过库存，改它等于把这笔账抹掉。红字单挑原单上的行、照它的位置、数量取负、类型
   * 照抄原单、指回原单。一次只冲一部分也行——同一张原单能开好几张红字单，今天冲 20、过两天再冲 10。
   *
   * <p>护栏只有一条：同一位置累计冲掉的不超过原单当初记的量。算「已冲多少」时，已经开出来的红字单都算数
   * （草稿也算——草稿虽然还没搬货，可它已经把这部分额度占住了）：草稿虽然还没搬货，可它已经把这部分额度占住了；不占的话两张草稿各冲满额，过账时
   * 谁也拦不住——仓里别的批次有货，第二张会去偷别人的账。
   *
   * <p>红字单本身不能再红冲：它当初就是往回冲的，要纠正它得另开一张正向单。
   *
   * @param id 被冲的原单Id
   * @param remark 备注（可空，写冲销原因）
   * @param lines 要冲掉的行与量（数量填正数）
   * @return 红字单（草稿，由调用方保存与过账）
   * @throws DomainException 原单不存在、还没过账、本身是红字单、没给行、行对不上原单、或累计冲超原量时抛出
   */
  public StockEntry reverse(
      long id,
      @Nullable String remark,
      List<StockEntryLineData> lines) {
    StockEntry origin = getExist(id);
    if (!origin.isSubmitted()) {
      throw new DomainException("库存单据" + origin.getNumber() + "还没过账，作废就行，不用红冲");
    }
    if (origin.isReversal()) {
      throw new DomainException("库存单据" + origin.getNumber() + "本身就是红字单，不能再红冲");
    }
    if (lines.isEmpty()) {
      throw new DomainException("红字单得有行：说清冲原单的哪几行、各冲多少");
    }
    List<StockEntry> reversals =
        stockEntryRepository.getListByReversalOfStockEntryId(origin.getId());
    StockEntry model =
        new StockEntry(
            origin.getStockEntryType(),
            origin.getSourceType(),
            origin.getSourceId(),
            origin.getSourceNumber(),
            remark,
            "",
            origin.getId());
    for (StockEntryLineData data : lines) {
      if (data.qty().signum() <= 0) {
        throw new DomainException("冲多少要填正数，负号由单子自己落");
      }
      StockEntryLine originLine = getOriginLine(origin, data);
      BigDecimal remained = getRemainedQty(originLine, reversals);
      if (data.qty().compareTo(remained) > 0) {
        throw new DomainException(
            "物料" + data.variantId() + "在原单上只剩" + remained + "能冲，填了" + data.qty());
      }
      model.addLine(
          data.materialId(),
          data.variantId(),
          data.qty().negate(),
          data.sourceWarehouseId(),
          data.targetWarehouseId(),
          data.remark());
    }
    return model;
  }

  // ==================== 私有 ====================

  /**
   * 按行上的仓推单据类型编码
   *
   * <p>规则照 ERPNext 的 {@code make_stock_entry}：源仓与目标仓都填是调拨，只填源仓是领料出库，只填目标仓
   * 是收货入库。一张单里各行推出的编码必须一致——既出又入的单要拆成两张开，混在一张里归堆和红冲都说不清。
   *
   * @param lines 库存单据行数据（非空）
   * @return 类型编码
   * @throws DomainException 某行源仓与目标仓都空（行构造器已拦）或各行编码不一致时抛出
   */
  private String resolveCode(List<StockEntryLineData> lines) {
    String code = null;
    for (StockEntryLineData data : lines) {
      String lineCode;
      if (data.sourceWarehouseId() != null && data.targetWarehouseId() != null) {
        lineCode = StockEntryTypeEnum.MATERIAL_TRANSFER.getCode();
      } else if (data.sourceWarehouseId() != null) {
        lineCode = StockEntryTypeEnum.MATERIAL_ISSUE.getCode();
      } else {
        lineCode = StockEntryTypeEnum.MATERIAL_RECEIPT.getCode();
      }
      if (code == null) {
        code = lineCode;
      } else if (!code.equals(lineCode)) {
        throw new DomainException("一张单里既有领料又有入库，说不清是哪种单，请分开开单");
      }
    }
    return code;
  }

  /**
   * 按编码取标准类型行，不存在或已停用则抛异常
   *
   * @param code 类型编码
   * @return 标准类型行
   * @throws DomainException 标准行缺失或已停用时抛出
   */
  private StockEntryType getStandardType(String code) {
    StockEntryType model = stockEntryTypeRepository.getStandardByCode(code);
    if (model == null) {
      throw new DomainException(
          "类型编码" + code + "没有预置的标准类型行，请检查初始化数据");
    }
    if (model.getDisabled()) {
      throw new DomainException("库存单据类型" + model.getName() + "已停用，开不了单");
    }
    return model;
  }

  /**
   * 在原单里找同位置的那一行
   *
   * @param origin 被冲的原单
   * @param data 要冲的行
   * @return 原单上的行
   * @throws DomainException 原单上没有这一行时抛出
   */
  private StockEntryLine getOriginLine(StockEntry origin, StockEntryLineData data) {
    for (StockEntryLine line : origin.getStockEntryLines()) {
      if (isSamePosition(
          line, data.variantId(), data.sourceWarehouseId(), data.targetWarehouseId())) {
        return line;
      }
    }
    throw new DomainException(
        "原单上没有物料"
            + data.variantId()
            + "在这对仓库之间的这一行，冲不了");
  }

  /**
   * 算原单某一行还剩多少能冲
   *
   * @param originLine 原单上的行
   * @param reversals 该原单名下已开出来的红字单（含草稿）
   * @return 还能冲掉的量
   */
  private BigDecimal getRemainedQty(
      StockEntryLine originLine, List<StockEntry> reversals) {
    BigDecimal reversed = BigDecimal.ZERO;
    for (StockEntry reversal : reversals) {
      for (StockEntryLine line : reversal.getStockEntryLines()) {
        if (isSamePosition(line, originLine)) {
          reversed = reversed.add(line.getQty());
        }
      }
    }
    return originLine.getQty().add(reversed);
  }

  /**
   * 比位置：物料、源仓、目标仓三项都对上，才算同一行
   *
   * @param line 待比的行
   * @param variantId 物料SKU
   * @param sourceWarehouseId 源仓库Id（可空）
   * @param targetWarehouseId 目标仓库Id（可空）
   * @return true 表示同一位置
   */
  private boolean isSamePosition(
      StockEntryLine line,
      long variantId,
      @Nullable Long sourceWarehouseId,
      @Nullable Long targetWarehouseId) {
    return Objects.equals(line.getVariantId(), variantId)
        && Objects.equals(line.getSourceWarehouseId(), sourceWarehouseId)
        && Objects.equals(line.getTargetWarehouseId(), targetWarehouseId);
  }

  /**
   * 比位置：另一行与本行是不是同一个位置
   *
   * @param line 待比的行
   * @param other 另一行
   * @return true 表示同一位置
   */
  private boolean isSamePosition(StockEntryLine line, StockEntryLine other) {
    return isSamePosition(
        line,
        other.getVariantId(),
        other.getSourceWarehouseId(),
        other.getTargetWarehouseId());
  }

  /**
   * 取库存单据，不存在则抛异常
   *
   * @param id 库存单据Id
   * @return 库存单据聚合
   * @throws DomainException 库存单据不存在时抛出
   */
  private StockEntry getExist(long id) {
    StockEntry model = stockEntryRepository.get(id);
    if (model == null) {
      throw new DomainException("库存单据" + id + "不存在!");
    }
    return model;
  }
}
