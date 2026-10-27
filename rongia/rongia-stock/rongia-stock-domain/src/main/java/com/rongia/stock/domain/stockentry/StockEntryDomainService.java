package com.rongia.stock.domain.stockentry;

import com.rongia.stock.domain.stockentrytype.StockEntryDirectionEnum;
import com.rongia.stock.domain.stockentrytype.StockEntryType;
import com.rongia.stock.domain.stockentrytype.StockEntryTypeEnum;
import com.rongia.stock.domain.stockentrytype.StockEntryTypeRepository;
import com.rongia.shared.domain.enums.SerialNumberTypeEnum;
import com.rongia.shared.domain.exception.DomainException;
import com.rongia.shared.domain.util.EnumUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 库存单据领域服务
 *
 * <p>类型不用人选：开单时按行上的库位推出编码（出库库位+入库库位都填是调拨、只填出库库位是出库、只填入库库位是
 * 入库），再按编码反查类型表里的标准行落到单头上——ERPNext 的 {@code make_stock_entry} 原样照搬的链路。
 * 来源单据可空——系统照业务单生成的单子带来源，车间手工开的单不填。红冲是唯一能凭空开出一张单据的动作，
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
    return create(null, null, null, typeCode, remark, lines);
  }

  /**
   * 开一张库存单据（草稿）
   *
   * <p>单号不接入参：由 OrderAddedEvent 的监听器按流水号规则分配。
   *
   * <p>类型有两条路：{@code typeCode} 传空时按行上的库位与来源推（程序自动开单走这条，只可能推出四个标准编码）；
   * 传了就按它落，用于手工开单挂自建类型。方向照样由类型决定，库位填错一样拦。
   *
   * @param sourceType 来源单据类型编码（可空，手工开单不填）
   * @param sourceId 来源单据Id（可空）
   * @param sourceNumber 来源单据号（快照，可空）
   * @param typeCode 类型编码（可空，空则按行推）
   * @param remark 备注（可空）
   * @param lines 库存单据行数据
   * @return 库存单据聚合
   * @throws DomainException 行集为空、各行推不出同一个编码、类型行缺失或已停用、或来源只填一半时抛出
   */
  public StockEntry create(
      @Nullable String sourceType,
      @Nullable Long sourceId,
      @Nullable String sourceNumber,
      @Nullable String typeCode,
      @Nullable String remark,
      List<StockEntryLineData> lines) {
    if (lines.isEmpty()) {
      throw new DomainException("库存单据得有行：说清哪个SKU从哪个库位到哪个库位");
    }
    String code = typeCode == null ? resolveCode(lines, sourceType) : typeCode;
    validateLineShape(resolveDirection(code), lines);
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
          data.outboundLocationId(),
          data.inboundLocationId(),
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
          data.outboundLocationId(),
          data.inboundLocationId(),
          data.remark());
    }
    return model;
  }

  // ==================== 私有 ====================

  /**
   * 按行上的仓推单据类型编码
   *
   * <p>规则照 ERPNext 的 {@code make_stock_entry}：出库库位与入库库位都填是调拨，只填出库库位是出库，只填入库库位
   * 是入库。一张单里各行推出的编码必须一致——既出又入的单要拆成两张开，混在一张里归堆和红冲都说不清。
   *
   * @param lines 库存单据行数据（非空）
   * @return 类型编码
   * @throws DomainException 某行出库库位与入库库位都空（行构造器已拦）或各行编码不一致时抛出
   */
  private String resolveCode(List<StockEntryLineData> lines, @Nullable String sourceType) {
    String code = null;
    for (StockEntryLineData data : lines) {
      String lineCode;
      if (data.outboundLocationId() != null && data.inboundLocationId() != null) {
        lineCode = StockEntryTypeEnum.TRANSFER.getCode();
      } else if (data.outboundLocationId() != null) {
        lineCode = StockEntryTypeEnum.PRODUCTION_ISSUE.getCode();
      } else {
        lineCode =
            SerialNumberTypeEnum.WORK_ORDER.getCode().equals(sourceType)
                ? StockEntryTypeEnum.PRODUCTION_RECEIPT.getCode()
                : StockEntryTypeEnum.PURCHASE_RECEIPT.getCode();
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
   * 按编码取方向
   *
   * <p>编码落在 {@link StockEntryTypeEnum} 里，方向由枚举直接给（程序自动开单推出来的编码都在这四个里）；
   * 不在枚举里，说明是用户自建的类型，查类型表读它的 {@code Direction}。
   *
   * @param code 类型编码
   * @return 方向编码
   * @throws DomainException 类型行不存在或已停用时抛出
   */
  private String resolveDirection(String code) {
    StockEntryTypeEnum known = EnumUtils.fromCode(StockEntryTypeEnum.class, code);
    if (known != null) {
      return known.getDirection().getCode();
    }
    StockEntryType model = stockEntryTypeRepository.getByCode(code);
    if (model == null) {
      throw new DomainException("类型编码" + code + "不存在，请先建类型");
    }
    if (model.getDisabled()) {
      throw new DomainException("库存单据类型" + model.getName() + "已停用，开不了单");
    }
    return model.getDirection();
  }

  /**
   * 按类型校验每一行的库位形状
   *
   * <p>方向决定哪些库位必须有：出入两头都要，出库只要出库库位，入库只要入库库位。
   * 校验只看方向、不看类型叫什么名字——用户自建的类型只要选了方向，规则自动生效。
   *
   * @param direction 方向编码
   * @param lines 库存单据行数据
   * @throws DomainException 行的库位形状与方向不符时抛出
   */
  private void validateLineShape(String direction, List<StockEntryLineData> lines) {
    for (StockEntryLineData data : lines) {
      boolean hasOutbound = data.outboundLocationId() != null;
      boolean hasInbound = data.inboundLocationId() != null;
      if (StockEntryDirectionEnum.BOTH.getCode().equals(direction)) {
        if (!hasOutbound || !hasInbound) {
          throw new DomainException("出入的行必须同时填出库库位和入库库位");
        }
      } else if (StockEntryDirectionEnum.OUT.getCode().equals(direction)) {
        if (!hasOutbound) {
          throw new DomainException("出库的行必须填出库库位");
        }
        if (hasInbound) {
          throw new DomainException("出库的行不能填入库库位，两个都填请开调拨单");
        }
      } else if (StockEntryDirectionEnum.IN.getCode().equals(direction)) {
        if (!hasInbound) {
          throw new DomainException("入库的行必须填入库库位");
        }
        if (hasOutbound) {
          throw new DomainException("入库的行不能填出库库位，两个都填请开调拨单");
        }
      }
    }
  }

  /**
   * 按编码取标准类型行，不存在或已停用则抛异常
   *
   * @param code 类型编码
   * @return 标准类型行
   * @throws DomainException 标准行缺失或已停用时抛出
   */
  private StockEntryType getStandardType(String code) {
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
        other.getOutboundLocationId(),
        other.getInboundLocationId());
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
