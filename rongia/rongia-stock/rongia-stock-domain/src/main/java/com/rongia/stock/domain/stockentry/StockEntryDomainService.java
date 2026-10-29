package com.rongia.stock.domain.stockentry;

import com.rongia.stock.domain.stockentrytype.StockEntryDirectionEnum;
import com.rongia.stock.domain.stockentrytype.StockEntryType;
import com.rongia.stock.domain.stockentrytype.StockEntryTypeEnum;
import com.rongia.stock.domain.stockentrytype.StockEntryTypeRepository;
import com.rongia.shared.domain.enums.SerialNumberTypeEnum;
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
 * <p>类型有两条路，各只有一处来源：程序自动开单按行上的库位推编码（出库库位+入库库位都填是调拨、只填出库库位是
 * 出库、只填入库库位是入库），推出来直接落在单头上，不查类型表——方向本来就是行形状说出来的；手工开单反过来，
 * 人挑一个类型编码传进来，查类型表确认它存在、没停用，再按它表上的方向检查行的库位填法。
 *
 * <p>来源单据可空——系统照业务单生成的单子带来源，车间手工开的单不填。红冲是唯一能凭空开出一张单据的动作，
 * 而它也只能照着已完成的原单开——挑原单上的行，照它的位置，数量取负，类型照抄原单，冲多少由开单的人定。
 *
 * <p>库存加减不在这里：过账这一步按行调 {@code StockDomainService} 入库或出库，由应用层编排，本模块只保证
 * 单据自己那半落定。红字单也一样，开出来先是草稿：过不过得去账取决于库里还有没有货，过不去就让它停在草稿挂着，
 * 等人先去处理下游那张单。
 *
 * <p>库位一律必填其一：出库库位或入库库位至少一个，方向由「填了哪一端」决定。红字单照抄原单的库位，不受这条影响。
 */
@NullMarked
public class StockEntryDomainService {

  /** 库存单据仓储 */
  private final StockEntryRepository stockEntryRepository;

  /** 库存单据类型仓储：开单时按类型带出方向 */
  private final StockEntryTypeRepository stockEntryTypeRepository;

  /**
   * 构造器注入
   *
   * @param stockEntryRepository 库存单据仓储
   * @param stockEntryTypeRepository 库存单据类型仓储
   */
  public StockEntryDomainService(
      StockEntryRepository stockEntryRepository,
      StockEntryTypeRepository stockEntryTypeRepository) {
    this.stockEntryRepository = stockEntryRepository;
    this.stockEntryTypeRepository = stockEntryTypeRepository;
  }

  /**
   * 手工开一张库存单据（草稿）：没有来源单据，类型由人指定
   *
   * @param typeCode 类型编码
   * @param remark 备注（可空）
   * @param lines 库存单据行数据
   * @return 库存单据聚合
   * @throws DomainException 行集为空、类型行缺失或已停用、或库位形状与类型方向不符时抛出
   */
  public StockEntry create(
      String typeCode, @Nullable String remark, List<StockEntryLineData> lines) {
    if (lines.isEmpty()) {
      throw new DomainException("库存单据得有行：说清哪个SKU从哪个库位到哪个库位");
    }
    StockEntryType type = getUsableType(typeCode);
    validateLineShape(type.getDirection(), lines);
    return build(type.getCode(), null, null, null, remark, lines);
  }

  /**
   * 开一张库存单据（草稿）
   *
   * <p>单号不接入参：由 OrderAddedEvent 的监听器按流水号规则分配。
   *
   * <p>{@code sourceType} 与来源单据号是程序这条路的入口：编码按行上的库位与来源推，推出来只可能是那七个
   * 标准编码，方向由行的形状决定，不查类型表。手工开单走另一条重载，类型由人挑。
   *
   * @param sourceType 来源单据类型编码，取值见 {@link SerialNumberTypeEnum}
   * @param sourceId 来源单据Id
   * @param sourceNumber 来源单据号（快照）
   * @param remark 备注（可空）
   * @param lines 库存单据行数据
   * @return 库存单据聚合
   * @throws DomainException 行集为空、或各行推不出同一个编码时抛出
   */
  public StockEntry create(
      String sourceType,
      long sourceId,
      String sourceNumber,
      @Nullable String remark,
      List<StockEntryLineData> lines) {
    if (lines.isEmpty()) {
      throw new DomainException("库存单据得有行：说清哪个SKU从哪个库位到哪个库位");
    }
    return build(
        resolveCode(lines, sourceType), sourceType, sourceId, sourceNumber, remark, lines);
  }

  /**
   * 装出一张库存单据
   *
   * <p>单号传空串：由 OrderAddedEvent 的监听器按流水号规则分配。
   *
   * @param typeCode 单据类型编码（已定）
   * @param sourceType 来源单据类型编码（可空，手工开单没有来源）
   * @param sourceId 来源单据Id（可空）
   * @param sourceNumber 来源单据号（快照，可空）
   * @param remark 备注（可空）
   * @param lines 库存单据行数据
   * @return 库存单据聚合
   */
  private StockEntry build(
      String typeCode,
      @Nullable String sourceType,
      @Nullable Long sourceId,
      @Nullable String sourceNumber,
      @Nullable String remark,
      List<StockEntryLineData> lines) {
    StockEntry model =
        new StockEntry(typeCode, sourceType, sourceId, sourceNumber, remark, "", null);
    for (StockEntryLineData data : lines) {
      model.addLine(
          data.materialId(),
          data.variantId(),
          data.qty(),
          data.outboundLocationId(),
          data.inboundLocationId(),
          null,
          data.remark());
    }
    return model;
  }

  /**
   * 过账：单据转入完成
   *
   * <p>按行加减库存由应用层在同一个事务里完成，本方法只把单据落定。
   *
   * @param id 库存单据Id
   * @param time 过账时间
   * @return 库存单据聚合
   * @throws DomainException 单据不存在、已过账、或单内没有行时抛出
   */
  public StockEntry post(long id, LocalDateTime time) {
    StockEntry model = getExist(id);
    model.post(time);
    return model;
  }

  /**
   * 取消：草稿作废，单据转入已取消
   *
   * @param id 库存单据Id
   * @return 库存单据聚合
   * @throws DomainException 单据不存在、或已过账/已取消时抛出
   */
  public StockEntry cancel(long id) {
    StockEntry model = getExist(id);
    model.cancel();
    return model;
  }

  /**
   * 红冲：照已完成的原单开一张红字单，只冲指定的几行几量
   *
   * <p>原单不动：它当初真的动过库存，改它等于把这笔账抹掉。红字单挑原单上的行、照它的位置、数量取负、类型
   * 照抄原单、指回原单。一次只冲一部分也行——同一张原单能开好几张红字单，今天冲 20、过两天再冲 10。
   *
   * <p>护栏只有一条：同一位置累计冲掉的不超过原单当初记的量。算「已冲多少」时，已经开出来的红字单都算数
   * （草稿也算——草稿虽然还没搬货，可它已经把这部分额度占住了）；不占的话两张草稿各冲满额，过账时
   * 谁也拦不住——库位里别的批次有货，第二张会去偷别人的账。
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
      if (findReversedLine(model, originLine.getId()) != null) {
        throw new DomainException("物料" + data.variantId() + "的原行在同一张红字单里冲了两次");
      }
      BigDecimal remained = getRemainedQty(originLine, reversals);
      if (data.qty().compareTo(remained) > 0) {
        throw new DomainException(
            "物料" + data.variantId() + "在原单上只剩" + remained + "能冲，填了" + data.qty());
      }
      model.addLine(
          data.materialId(),
          data.variantId(),
          data.qty().negate(),
          data.outboundLocationId(),
          data.inboundLocationId(),
          originLine.getId(),
          data.remark());
    }
    return model;
  }

  // ==================== 私有 ====================

  /**
   * 按行上的库位与来源推单据类型编码
   *
   * <p>规则照 ERPNext 的 {@code make_stock_entry}：出库库位与入库库位都填是调拨；只填一端时再看来源——
   * 出库是销售单就是销售出库、是采购单就是采购退货、否则是领料出库；入库是销售单就是销售退货、是工单就是完工入库、
   * 否则是采购入库。一张单里各行推出的编码必须一致——既出又入的单要拆成两张开，混在一张里归堆和红冲都说不清。
   *
   * <p>编码从第一行起就有：调用方已经拦过空行集，第一行推出来的就是这张单的编码，不存在推不出来的情况。
   *
   * @param lines 库存单据行数据（非空）
   * @param sourceType 来源单据类型编码（程序这条路必有来源）
   * @return 类型编码（必非空）
   * @throws DomainException 各行编码不一致时抛出
   */
  private String resolveCode(List<StockEntryLineData> lines, String sourceType) {
    String code = resolveLineCode(lines.get(0), sourceType);
    for (StockEntryLineData data : lines) {
      if (!code.equals(resolveLineCode(data, sourceType))) {
        throw new DomainException("一张单里既有领料又有入库，说不清是哪种单，请分开开单");
      }
    }
    return code;
  }

  /**
   * 按一行的库位与来源推这条行的编码
   *
   * <p>只填一端时，出库与入库都要看来源：同样是「只有出库库位」，来源是销售单是销售出库、是采购单是采购退货、
   * 其余是领料出库；同样是「只有入库库位」，来源是销售单是销售退货、是工单是完工入库、其余是采购入库。
   *
   * @param data 库存单据行数据
   * @param sourceType 来源单据类型编码
   * @return 该行对应的类型编码
   */
  private String resolveLineCode(StockEntryLineData data, String sourceType) {
    if (data.outboundLocationId() != null && data.inboundLocationId() != null) {
      return StockEntryTypeEnum.TRANSFER.getCode();
    }
    if (data.outboundLocationId() != null) {
      if (SerialNumberTypeEnum.SALES_ORDER.getCode().equals(sourceType)) {
        return StockEntryTypeEnum.SALES_DELIVERY.getCode();
      }
      return SerialNumberTypeEnum.PURCHASE_ORDER.getCode().equals(sourceType)
          ? StockEntryTypeEnum.PURCHASE_RETURN.getCode()
          : StockEntryTypeEnum.PRODUCTION_ISSUE.getCode();
    }
    if (SerialNumberTypeEnum.SALES_ORDER.getCode().equals(sourceType)) {
      return StockEntryTypeEnum.SALES_RETURN.getCode();
    }
    return SerialNumberTypeEnum.WORK_ORDER.getCode().equals(sourceType)
        ? StockEntryTypeEnum.PRODUCTION_RECEIPT.getCode()
        : StockEntryTypeEnum.PURCHASE_RECEIPT.getCode();
  }

  /**
   * 按编码取可用的类型行
   *
   * <p>手工开单走这里：编码得是表里已有的自建类型，还得没停用。程序保留的七个编码按设计不在表里（用户建不出来），
   * 手工开单也就选不到它们——那七个由程序自动开单那条路直接用，不查表。
   *
   * @param code 类型编码
   * @return 类型行
   * @throws DomainException 类型行不存在或已停用时抛出
   */
  private StockEntryType getUsableType(String code) {
    StockEntryType model = stockEntryTypeRepository.getByCode(code);
    if (model == null) {
      throw new DomainException("类型编码" + code + "不存在，请先建类型");
    }
    if (model.getDisabled()) {
      throw new DomainException("库存单据类型" + model.getName() + "已停用，开不了单");
    }
    return model;
  }

  /**
   * 按类型校验每一行的库位形状
   *
   * <p>方向决定哪些库位必须有：出库只要出库库位、入库只要入库库位。方向属于整张单，一张单要么都入、
   * 要么都出。校验只看方向、不看类型叫什么名字——用户自建的类型只要选了方向，规则自动生效。
   *
   * @param direction 方向编码
   * @param lines 库存单据行数据
   * @throws DomainException 行的库位形状与方向不符时抛出
   */
  private void validateLineShape(String direction, List<StockEntryLineData> lines) {
    for (StockEntryLineData data : lines) {
      boolean hasOutbound = data.outboundLocationId() != null;
      boolean hasInbound = data.inboundLocationId() != null;
      if (hasOutbound && hasInbound) {
        throw new DomainException("不能同时填出库库位与入库库位，调拨请开调拨单");
      }
      if (StockEntryDirectionEnum.OUT.getCode().equals(direction)) {
        if (!hasOutbound) {
          throw new DomainException("出库的行必须填出库库位");
        }
      } else if (StockEntryDirectionEnum.IN.getCode().equals(direction)) {
        if (!hasInbound) {
          throw new DomainException("入库的行必须填入库库位");
        }
      }
    }
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
          line, data.variantId(), data.outboundLocationId(), data.inboundLocationId())) {
        return line;
      }
    }
    throw new DomainException(
        "原单上没有物料"
            + data.variantId()
            + "在这对库位之间的这一行，冲不了");
  }

  /**
   * 在红字单里找已经指回某原行的那一行
   *
   * <p>同一原行在同一张红字单里只许出现一次：冲两次的话「已冲多少」要把同一笔算两遍，额度凭空多出来。
   * 红字单的 addLine 那条按位置查重挡不住这个——两行都是照同一原行开的，位置本来就一样。
   *
   * @param reversal 正在开的红字单
   * @param originLineId 原行Id
   * @return 已指回该原行的那一行，找不到返回 null
   */
  @Nullable
  private StockEntryLine findReversedLine(StockEntry reversal, long originLineId) {
    for (StockEntryLine line : reversal.getStockEntryLines()) {
      if (Objects.equals(line.getReversalOfStockEntryLineId(), originLineId)) {
        return line;
      }
    }
    return null;
  }

  /**
   * 算原单某一行还剩多少能冲
   *
   * <p>红字行都是照着原行开出来的，行上记着原行Id，所以直接认这个指针，不再比位置。
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
        if (originLine.getId().equals(line.getReversalOfStockEntryLineId())) {
          reversed = reversed.add(line.getQty());
        }
      }
    }
    return originLine.getQty().add(reversed);
  }

  /**
   * 比位置：物料、出库库位、入库库位三项都对上，才算同一行
   *
   * @param line 待比的行
   * @param variantId 物料SKU
   * @param outboundLocationId 出库库位Id（可空）
   * @param inboundLocationId 入库库位Id（可空）
   * @return true 表示同一位置
   */
  private boolean isSamePosition(
      StockEntryLine line,
      long variantId,
      @Nullable Long outboundLocationId,
      @Nullable Long inboundLocationId) {
    return Objects.equals(line.getVariantId(), variantId)
        && Objects.equals(line.getOutboundLocationId(), outboundLocationId)
        && Objects.equals(line.getInboundLocationId(), inboundLocationId);
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
