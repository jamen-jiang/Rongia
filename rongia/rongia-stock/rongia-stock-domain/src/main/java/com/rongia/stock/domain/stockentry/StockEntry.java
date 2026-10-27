package com.rongia.stock.domain.stockentry;

import com.rongia.stock.domain.stockentrytype.StockEntryType;
import com.rongia.shared.domain.aggregate.OrderAggregateRoot;
import com.rongia.shared.domain.enums.SerialNumberTypeEnum;
import com.rongia.shared.domain.exception.DomainException;
import com.rongia.shared.domain.util.EnumUtils;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.SQLRestriction;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 库存单据（一次库存动账）
 *
 * <p>领料、完工入库、调拨、报废——仓里发生的每一种动作都是这一张单：开单时从头上的「单据类型」里挑一个种类
 * （类型编码按行上的仓自动推出来，类型行在 {@code StockEntryType} 表里），行上填哪个 SKU、从哪个仓到哪个仓，过账后仓里的数就变了。
 * 类型做成配置表而不是把业务各建一张单，加一种「报废出库」是运行时多一行配置的事，表结构和代码都不用动
 * （ERPNext 的 {@code Stock Entry} + {@code Stock Entry Type} 就是这个形状）。
 *
 * <p>来源单据是可选的：系统照着业务单（工单、报工单）生成的单子带来源，指回「是哪张单让我动的」；车间现场
 * 手工开的单没有来源，三列留空。三个来源列要么全填、要么全空，半填等于说不清缘由。
 *
 * <p>撤销走红冲，不是翻状态：过过账的原单从此冻住，照它开一张同方向、数量取负的红字单，用
 * {@code reversalOfStockEntryId} 指回原单，两张加一块是零。原单上不另存「已红冲」——那是查得出来的事实，存一列
 * 迟早对不上。冲也分次：一张单今天冲 20、过两天再冲 10，红字单挑哪几行、各冲多少由开单的人定，只要同一
 * 原单上同一位置累计不超过它当初记的量。只有还没过账的草稿才配取消：它一分货没动，没有要冲的东西。
 *
 * <p>库存加减不在这里：过账这一步的入库、出库由应用层调 {@code StockDomainService} 按行执行，本聚合只保证
 * 这张单自身说得清。红字单过不去账（比如成品仓不够）就停在草稿挂着，等人先处理下游那张单。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "StockEntry",
    indexes = {
      @Index(name = "IX_StockEntry_StockEntryType", columnList = "StockEntryType"),
      @Index(name = "IX_StockEntry_SourceType_SourceId", columnList = "SourceType,SourceId"),
      @Index(name = "IX_StockEntry_Status", columnList = "Status"),
      @Index(name = "IX_StockEntry_ReversalOfStockEntryId", columnList = "ReversalOfStockEntryId")
    })
public class StockEntry extends OrderAggregateRoot {

  /** 单据类型（存类型编码，对应 {@code StockEntryType} 表的 Code 列） */
  @Nationalized
  @Column(name = "StockEntryType", nullable = false, length = 50)
  private String stockEntryType;

  /** 来源单据类型编码，取值见 {@link SerialNumberTypeEnum}（可空，手工开单没有来源） */
  @Nationalized
  @Column(name = "SourceType", length = 50)
  @Nullable
  private String sourceType;

  /** 来源单据Id（可空） */
  @Column(name = "SourceId")
  @Nullable
  private Long sourceId;

  /** 来源单据号（快照，可空，单据要能独立打印与核对） */
  @Nationalized
  @Column(name = "SourceNumber", length = 30)
  @Nullable
  private String sourceNumber;

  /** 状态：草稿、完成、已取消 */
  @Nationalized
  @Column(name = "Status", nullable = false, length = 50)
  private String status;

  /** 过账时间（可空，转入完成时写入，照 Odoo 的 date_done） */
  @Column(name = "DoneTime")
  @Nullable
  private LocalDateTime doneTime;

  /** 被冲的原单Id（可空，非空即本单是红字单） */
  @Column(name = "ReversalOfStockEntryId")
  @Nullable
  private Long reversalOfStockEntryId;

  /** 备注（可空，红字单上写冲销原因） */
  @Nationalized
  @Column(name = "Remark", length = 500)
  @Nullable
  private String remark;

  /** 库存单据行 */
  @OneToMany(
      fetch = FetchType.EAGER,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE},
      orphanRemoval = true)
  @JoinColumn(name = "StockEntryId")
  @SQLRestriction("IsDeleted = 0")
  private List<StockEntryLine> stockEntryLines = new ArrayList<>();

  /**
   * 构造器
   *
   * @param stockEntryType 单据类型编码
   * @param sourceType 来源单据类型编码（可空）
   * @param sourceId 来源单据Id（可空）
   * @param sourceNumber 来源单据号（快照，可空）
   * @param remark 备注（可空）
   * @param number 单号（传空串表示由 OrderAddedEvent 的监听器后续分配）
   * @param reversalOfStockEntryId 被冲的原单Id（可空，普通单据传 null）
   */
  StockEntry(
      String stockEntryType,
      @Nullable String sourceType,
      @Nullable Long sourceId,
      @Nullable String sourceNumber,
      @Nullable String remark,
      String number,
      @Nullable Long reversalOfStockEntryId) {
    super(number);
    apply(stockEntryType, sourceType, sourceId, sourceNumber, remark);
    if (reversalOfStockEntryId != null && reversalOfStockEntryId <= 0) {
      throw new DomainException("红字单要指回被冲的原单!");
    }
    this.reversalOfStockEntryId = reversalOfStockEntryId;
    this.status = StockEntryStatusEnum.DRAFT.getCode();
  }

  /**
   * 流水号类型
   *
   * @return 库存单据类型（红字单同用这一段号）
   */
  @Override
  public SerialNumberTypeEnum getSerialNumberType() {
    return SerialNumberTypeEnum.STOCK_ENTRY;
  }

  /**
   * 获取库存单据行（只读）
   *
   * @return 不可变列表
   */
  public List<StockEntryLine> getStockEntryLines() {
    return Collections.unmodifiableList(stockEntryLines);
  }

  /**
   * 添加库存单据行（仅草稿可加）
   *
   * <p>带符号数量只在这一个入口检查：负数等于「把一笔已过账的账倒回来」，所以只有红字单开得出来，普通单据要撤销
   * 得走红冲，不能自己填个负数；反过来红字单也不许填正数，它的量永远是往回冲的。开红字单的人只填「要冲掉多少」，
   * 那个负号由领域服务落。
   *
   * @param materialId 物料SPU（快照）
   * @param variantId 物料SKU
   * @param qty 数量（普通单为正，红字单为负）
   * @param outboundLocationId 出库库位Id（可空，与入库库位至少填一个）
   * @param inboundLocationId 入库库位Id（可空，与出库库位至少填一个）
   * @param remark 备注（可空）
   * @throws DomainException 非草稿状态、正负号与本单性质不符、或同一流转已在单内时抛出
   */
  public void addLine(
      long materialId,
      long variantId,
      BigDecimal qty,
      @Nullable Long outboundLocationId,
      @Nullable Long inboundLocationId,
      @Nullable String remark) {
    ensureDraft();
    if (this.reversalOfStockEntryId == null && qty.signum() < 0) {
      throw new DomainException("只有红字单能开负数数量，要撤销已过账的单请走红冲");
    }
    if (this.reversalOfStockEntryId != null && qty.signum() > 0) {
      throw new DomainException("红字单的数量要往回冲，正数请开普通单");
    }
    if (findLine(variantId, outboundLocationId, inboundLocationId) != null) {
      throw new DomainException("物料" + variantId + "在同一对仓库之间已在本单中!");
    }
    stockEntryLines.add(
        new StockEntryLine(
            materialId, variantId, qty, outboundLocationId, inboundLocationId, remark));
  }

  /**
   * 过账：单据转入完成并记下过账时间
   *
   * <p>按行加减库存那一下由应用层在同一个事务里调库存服务完成，本方法只把单据落定，避免同一张单重复动账。
   *
   * @param time 过账时间
   * @throws DomainException 非草稿状态、或单内没有行时抛出
   */
  public void post(LocalDateTime time) {
    ensureDraft();
    if (stockEntryLines.isEmpty()) {
      throw new DomainException("库存单据没有行，不能过账");
    }
    this.status = StockEntryStatusEnum.DONE.getCode();
    this.doneTime = time;
  }

  /**
   * 取消：草稿作废，单据转入已取消
   *
   * <p>只有还没过账的单才配取消：它一分货没动，没有要冲的东西。已完成的原单永远停在完成，
   * 要撤销走红冲。取消不删记录——留着说明「有人开过这张单、后来作废了」。
   *
   * @throws DomainException 非草稿状态时抛出
   */
  public void cancel() {
    ensureDraft();
    this.status = StockEntryStatusEnum.CANCELLED.getCode();
  }

  /**
   * 是否草稿
   *
   * @return true 表示草稿
   */
  public boolean isDraft() {
    return StockEntryStatusEnum.DRAFT.getCode().equals(this.status);
  }

  /**
   * 是否已完成（已过账）
   *
   * @return true 表示完成
   */
  public boolean isSubmitted() {
    return StockEntryStatusEnum.DONE.getCode().equals(this.status);
  }

  /**
   * 是否已取消
   *
   * @return true 表示已取消
   */
  public boolean isCancelled() {
    return StockEntryStatusEnum.CANCELLED.getCode().equals(this.status);
  }

  /**
   * 是否红字单
   *
   * @return true 表示本单是冲掉另一张单的红字单
   */
  public boolean isReversal() {
    return this.reversalOfStockEntryId != null;
  }

  // ==================== 私有：校验与赋值 ====================

  /**
   * 统一的校验与赋值入口
   *
   * @param stockEntryType 单据类型编码
   * @param sourceType 来源单据类型编码（可空）
   * @param sourceId 来源单据Id（可空）
   * @param sourceNumber 来源单据号（快照，可空）
   * @param remark 备注（可空）
   * @throws DomainException 类型Id非法、来源填了一半或来源类型不在枚举内时抛出
   */
  private void apply(
      String stockEntryType,
      @Nullable String sourceType,
      @Nullable Long sourceId,
      @Nullable String sourceNumber,
      @Nullable String remark) {
    String trimmedEntryType = StringUtils.trimToNull(stockEntryType);
    if (trimmedEntryType == null) {
      throw new DomainException("单据类型不能为空");
    }
    String trimmedSourceType = StringUtils.trimToNull(sourceType);
    String trimmedSourceNumber = StringUtils.trimToNull(sourceNumber);
    boolean hasSource = trimmedSourceType != null || sourceId != null || trimmedSourceNumber != null;
    if (hasSource) {
      if (trimmedSourceType == null || sourceId == null) {
        throw new DomainException("来源要么整填、要么整空：只填一半就查不回是哪张单让这批货动的");
      }
      this.sourceType = EnumUtils.getCode(SerialNumberTypeEnum.class, trimmedSourceType, "来源单据类型");
      if (sourceId <= 0) {
        throw new DomainException("来源单据Id有误!");
      }
      this.sourceId = sourceId;
      this.sourceNumber = trimmedSourceNumber;
    } else {
      this.sourceType = null;
      this.sourceId = null;
      this.sourceNumber = null;
    }
    this.stockEntryType = trimmedEntryType;
    this.remark = remark;
  }

  /**
   * 校验单据仍处于草稿
   *
   * @throws DomainException 已完成或已取消时抛出
   */
  private void ensureDraft() {
    if (!isDraft()) {
      throw new DomainException("库存单据已过账或已取消，不能再修改");
    }
  }

  /**
   * 按「SKU + 出库库位 + 入库库位」查找未删除的行
   *
   * <p>同一张单里重复记同一对仓之间的同一次流转，等于把一笔账记两遍，过账时库存会多走一遍。
   *
   * @param variantId 物料SKU
   * @param outboundLocationId 出库库位Id（可空）
   * @param inboundLocationId 入库库位Id（可空）
   * @return 匹配到的行，找不到返回 {@code null}
   */
  @Nullable
  private StockEntryLine findLine(
      long variantId, @Nullable Long outboundLocationId, @Nullable Long inboundLocationId) {
    return stockEntryLines.stream()
        .filter(
            l ->
                Objects.equals(l.getVariantId(), variantId)
                    && Objects.equals(l.getOutboundLocationId(), outboundLocationId)
                    && Objects.equals(l.getInboundLocationId(), inboundLocationId))
        .findFirst()
        .orElse(null);
  }
}
