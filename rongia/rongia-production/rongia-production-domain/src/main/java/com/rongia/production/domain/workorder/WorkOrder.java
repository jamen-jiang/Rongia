package com.rongia.production.domain.workorder;

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
import java.util.Comparator;
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
 * 工单（生产订单）
 *
 * <p>工单是生产的执行单元：一张工单做且只做一个 SKU 的一个数量。销售订单行可以拆多张工单（分批投产）， 库存补货与返工也可以直接开单，所以来源用 {@code sourceType +
 * sourceId} 表达而不是绑死销售订单。
 *
 * <p>生命周期：{@code DRAFT → RELEASED → IN_PROGRESS → COMPLETED → CLOSED}，任意阶段可 {@code CANCELLED}。
 * 下达（{@link #release}）是唯一一次「定型」动作：按 BOM 展开用料、按工艺路线快照工序，之后路线改版不影响本单。
 *
 * <p>{@code materialId} 是产出 SKU 所属的 SPU，由开单方随 SKU 一起提交：一个系列下多个度数的工单要能折叠 到一起看，靠它分组。物料类型不在这里抄：它记在
 * {@code Variant} 主数据上，工单再存一份就成了两处说法， 而且「半成品/成品」这种分法在报表上并不比按 SPU 分组更准。
 *
 * <p>{@code deliveryTime} 是答应客户的承诺，属于需求侧，生产侧只读不写：它记的是当初答应的是哪天，改了就再也没 人知道。口径取自 SAP 的基本完成日期／计划完成日期、D365
 * 的交货日期。
 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "WorkOrder",
    indexes = {
      @Index(name = "IX_WorkOrder_SourceType_SourceId", columnList = "SourceType,SourceId"),
      @Index(name = "IX_WorkOrder_MaterialId", columnList = "MaterialId"),
      @Index(name = "IX_WorkOrder_VariantId", columnList = "VariantId"),
      @Index(name = "IX_WorkOrder_Status", columnList = "Status"),
    })
public class WorkOrder extends OrderAggregateRoot {

  /** 来源类型编码，取值见 {@link WorkOrderSourceTypeEnum} */
  @Nationalized
  @Column(name = "SourceType", nullable = false, length = 50)
  private String sourceType;

  /** 来源单据Id（可空，库存补货时为空） */
  @Column(name = "SourceId")
  @Nullable
  private Long sourceId;

  /** 来源单据行Id（可空：库存补货没有行；销售单拆工单时指到那一行，发货与开票靠它回写） */
  @Column(name = "SourceLineId")
  @Nullable
  private Long sourceLineId;

  /** 来源单号（可空，冗余存储便于追溯与展示） */
  @Nationalized
  @Column(name = "SourceNumber", length = 50)
  @Nullable
  private String sourceNumber;

  /** 产出SKU的SPU（同一系列多个度数的工单按它折叠） */
  @Column(name = "MaterialId", nullable = false)
  private Long materialId;

  /** 产出SKU */
  @Column(name = "VariantId", nullable = false)
  private Long variantId;

  /** 交期（可空，承诺的完工时间）：需求侧给的日期，生产侧只读不写 */
  @Column(name = "DeliveryTime")
  @Nullable
  private LocalDateTime deliveryTime;

  /** 计划开工时间（排产铺定，照 carbon 的 startDate） */
  @Column(name = "PlannedStartTime")
  @Nullable
  private LocalDateTime plannedStartTime;

  /** 预计完工时间（排产按工序链铺出来的结论，照 carbon 的 projectedCompletionAt） */
  @Column(name = "ProjectedEndTime")
  @Nullable
  private LocalDateTime projectedEndTime;

  /** 下达时刻（下达动作发生的时间，照 carbon 的 releasedDate） */
  @Column(name = "ReleasedTime")
  @Nullable
  private LocalDateTime releasedTime;

  /** 完工时刻（自动或手工结单的时间，照 carbon 的 completedDate） */
  @Column(name = "CompletedTime")
  @Nullable
  private LocalDateTime completedTime;

  /** 计划数量 */
  @Column(name = "Qty", nullable = false, precision = 18, scale = 2)
  private BigDecimal qty;

  /** 单位Id */
  @Column(name = "UnitId", nullable = false)
  private Long unitId;

  /** 收货库位Id（必填）：完工的产品进这里 */
  @Column(name = "LocationId", nullable = false)
  private Long locationId;

  /** 状态编码，取值见 {@link WorkOrderStatusEnum} */
  @Nationalized
  @Column(name = "Status", nullable = false, length = 50)
  private String status;

  /** 备注（可空） */
  @Nationalized
  @Column(name = "Remark", length = 500)
  @Nullable
  private String remark;

  /** 工单用料（下达时按 BOM 展开生成） */
  @OneToMany(
      fetch = FetchType.EAGER,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE},
      orphanRemoval = true)
  @JoinColumn(name = "WorkOrderId")
  @SQLRestriction("IsDeleted = 0")
  private List<WorkOrderLine> workOrderLines = new ArrayList<>();

  /** 工单工序（下达时按路线快照生成） */
  @OneToMany(
      fetch = FetchType.EAGER,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE},
      orphanRemoval = true)
  @JoinColumn(name = "WorkOrderId")
  @SQLRestriction("IsDeleted = 0")
  private List<WorkOrderOperation> workOrderOperations = new ArrayList<>();

  /**
   * 构造器
   *
   * @param sourceType 来源类型编码
   * @param sourceId 来源单据Id（可空）
   * @param sourceLineId 来源单据行Id（可空：库存补货没有行）
   * @param sourceNumber 来源单号（可空）
   * @param materialId 产出SKU的SPU
   * @param variantId 产出SKU
   * @param deliveryTime 交期（可空，承诺的完工时间）
   * @param plannedStartTime 计划开工时间（可空，排产时填）
   * @param qty 计划数量
   * @param unitId 单位Id
   * @param locationId 收货库位Id
   * @param remark 备注（可空）
   * @param number 单号（传空串表示由 OrderAddedEvent 的监听器后续分配）
   */
  WorkOrder(
      String sourceType,
      @Nullable Long sourceId,
      @Nullable Long sourceLineId,
      @Nullable String sourceNumber,
      long materialId,
      long variantId,
      @Nullable LocalDateTime deliveryTime,
      @Nullable LocalDateTime plannedStartTime,
      BigDecimal qty,
      long unitId,
      long locationId,
      @Nullable String remark,
      String number) {
    super(number);
    apply(
        sourceType,
        sourceId,
        sourceLineId,
        sourceNumber,
        materialId,
        variantId,
        deliveryTime,
        plannedStartTime,
        qty,
        unitId,
        locationId,
        remark);
    this.status = WorkOrderStatusEnum.DRAFT.getCode();
  }

  /**
   * 流水号类型
   *
   * @return 工单类型
   */
  @Override
  public SerialNumberTypeEnum getSerialNumberType() {
    return SerialNumberTypeEnum.WORK_ORDER;
  }

  /**
   * 获取工单用料（只读）
   *
   * @return 不可变列表
   */
  public List<WorkOrderLine> getWorkOrderLines() {
    return Collections.unmodifiableList(workOrderLines);
  }

  /**
   * 获取工单工序（只读）
   *
   * @return 不可变列表
   */
  public List<WorkOrderOperation> getWorkOrderOperations() {
    return Collections.unmodifiableList(workOrderOperations);
  }

  /**
   * 调整交期
   *
   * <p>交期是答应客户的承诺，属于需求侧，生产侧永不修改。
   *
   * @param deliveryTime 交期（可空）
   * @throws DomainException 工单已完工/已关闭/已取消时抛出
   */
  public void changeDeliveryTime(@Nullable LocalDateTime deliveryTime) {
    if (isCompleted() || isClosed() || isCancelled()) {
      throw new DomainException("工单已结束，不能再调整交期");
    }
    this.deliveryTime = deliveryTime;
    fillOperationDeliveryTimes();
  }

  /**
   * 回填预计完工时间（下达排产时由领域服务按工序链铺定）
   *
   * <p>只写预计列，不碰完工时刻——预计是按标准工时算出来的结论，实际结单另落一列，两者互不覆盖。
   *
   * @param projectedEndTime 预计完工时间
   */
  void fillProjectedEndTime(LocalDateTime projectedEndTime) {
    this.projectedEndTime = projectedEndTime;
  }

  /**
   * 按工单交期倒推每道工序的应完成时间
   *
   * <p>从末道往前减：末道的应完成时间就是工单交期，往前每道再减掉自己这道要花的工时。工单没有交期时，
   * 每道工序的应完成时间一并清空——没有承诺就没有「最晚什么时候做完」。
   */
  void fillOperationDeliveryTimes() {
    List<WorkOrderOperation> operations = orderedOperations();
    LocalDateTime due = this.deliveryTime;
    for (int index = operations.size() - 1; index >= 0; index--) {
      WorkOrderOperation operation = operations.get(index);
      operation.fillDeliveryTime(due);
      if (due != null) {
        due = due.minusMinutes(operation.estimatedMinutes(this.qty).longValue());
      }
    }
  }

  /**
   * 修改工单（仅草稿状态可改）
   *
   * @param sourceType 来源类型编码
   * @param sourceId 来源单据Id（可空）
   * @param sourceLineId 来源单据行Id（可空：库存补货没有行）
   * @param sourceNumber 来源单号（可空）
   * @param materialId 产出SKU的SPU
   * @param variantId 产出SKU
   * @param deliveryTime 交期（可空，承诺的完工时间）
   * @param plannedStartTime 计划开工时间（可空，排产时填）
   * @param qty 计划数量
   * @param unitId 单位Id
   * @param locationId 收货库位Id
   * @param remark 备注（可空）
   * @throws DomainException 非草稿状态时抛出
   */
  public void modify(
      String sourceType,
      @Nullable Long sourceId,
      @Nullable Long sourceLineId,
      @Nullable String sourceNumber,
      long materialId,
      long variantId,
      @Nullable LocalDateTime deliveryTime,
      @Nullable LocalDateTime plannedStartTime,
      BigDecimal qty,
      long unitId,
      long locationId,
      @Nullable String remark) {
    if (!isDraft()) {
      throw new DomainException("工单已下达，不能再修改");
    }
    apply(
        sourceType,
        sourceId,
        sourceLineId,
        sourceNumber,
        materialId,
        variantId,
        deliveryTime,
        plannedStartTime,
        qty,
        unitId,
        locationId,
        remark);
  }

  /**
   * 添加工序快照（仅草稿状态可加）
   *
   * <p>工序不保存计划投入数量：可投入多少取决于前置工序实际产出，报工时由本聚合现算，见 {@link #availableQtyFor}。
   *
   * @param operationId 工序Id
   * @param operationName 工序名称（快照）
   * @param workCenterId 工作中心Id
   * @param workCenterName 工作中心名称（快照）
   * @param setupTime 准备时间（分钟，每批一次）
   * @param laborTime 人工时间（分钟/件）
   * @param machineTime 机器时间（分钟/件）
   * @param laborRate 人工费率
   * @param machineRate 机器费率
   * @param overheadRate 制造费用率
   * @param sequence 工序顺序
   * @throws DomainException 非草稿状态、或工序顺序重复时抛出
   */
  public void addOperation(
      long operationId,
      String operationName,
      long workCenterId,
      String workCenterName,
      BigDecimal setupTime,
      BigDecimal laborTime,
      BigDecimal machineTime,
      BigDecimal laborRate,
      BigDecimal machineRate,
      BigDecimal overheadRate,
      int sequence) {
    if (!isDraft()) {
      throw new DomainException("工单已下达，不能再加工序");
    }
    if (findOperationBySequence(sequence) != null) {
      throw new DomainException("工序顺序" + sequence + "重复!");
    }
    workOrderOperations.add(
        new WorkOrderOperation(
            operationId,
            operationName,
            workCenterId,
            workCenterName,
            setupTime,
            laborTime,
            machineTime,
            laborRate,
            machineRate,
            overheadRate,
            sequence));
  }

  /**
   * 添加用料行（仅草稿状态可加）
   *
   * @param bomLineId 来源BOM行Id（可空）
   * @param materialId 物料SPU
   * @param variantId 物料SKU
   * @param routingOperationId 投放路线工序Id（可空，下达时从 BOM 行照抄）
   * @param requiredQty 需求数量
   * @param unitId 单位Id
   * @throws DomainException 非草稿状态时抛出
   */
  public void addLine(
      @Nullable Long bomLineId,
      long materialId,
      long variantId,
      @Nullable Long routingOperationId,
      BigDecimal requiredQty,
      long unitId) {
    if (!isDraft()) {
      throw new DomainException("工单已下达，不能再加用料");
    }
    workOrderLines.add(
        new WorkOrderLine(
            bomLineId, materialId, variantId, routingOperationId, requiredQty, unitId));
  }

  /**
   * 下达：把已装配的工序与用料固化，工单转入已下达
   *
   * <p>下达是幂等的边界：一旦下达，工序与用料就固化在本单内，工艺路线或 BOM 后续改版不影响本单。
   * 配方是整份抄进来的，单上不留「用了哪份 BOM/路线」的指针——要看配方长什么样，看单上的行。
   *
   * @param moment 下达时刻
   * @throws DomainException 非草稿状态、或没有任何工序时抛出
   */
  public void release(LocalDateTime moment) {
    if (!isDraft()) {
      throw new DomainException("只有草稿状态的工单才能下达");
    }
    if (workOrderOperations.isEmpty()) {
      throw new DomainException("工单没有工序，不能下达");
    }

    this.releasedTime = moment;
    this.status = WorkOrderStatusEnum.RELEASED.getCode();
  }

  /**
   * 开工：第一笔报工落下时由回写触发
   *
   * @throws DomainException 非已下达状态时抛出
   */
  private void start() {
    if (!isReleased()) {
      throw new DomainException("只有已下达的工单才能开工");
    }
    this.status = WorkOrderStatusEnum.IN_PROGRESS.getCode();
  }

  /**
   * 完工
   *
   * <p>正常路径是工序一道道收尾后自动转完工，这里保留手工入口用于「产出已达成计划数量、但还有
   * 工序挂在加工中」的车间收尾场景。不满足数量口径时不允许手工结单：产出加报废没凑够计划数，说明还有料 没交代去处，结单会把这笔账留在库存与成本上。
   *
   * @param moment 完工时刻
   * @throws DomainException 非生产中的工单、或产出加报废未达计划数量时抛出
   */
  public void complete(LocalDateTime moment) {
    if (!isInProgress()) {
      throw new DomainException("只有生产中的工单才能完工");
    }
    if (!isQtySettled()) {
      throw new DomainException("完工数量未达计划数量，不能完工");
    }
    this.status = WorkOrderStatusEnum.COMPLETED.getCode();
    this.completedTime = moment;
  }

  /**
   * 关闭
   *
   * @throws DomainException 非已完工状态时抛出
   */
  public void close() {
    if (!isCompleted()) {
      throw new DomainException("只有已完工的工单才能关闭");
    }
    this.status = WorkOrderStatusEnum.CLOSED.getCode();
  }

  /**
   * 取消
   *
   * @throws DomainException 已完工、已关闭或已取消时抛出
   */
  public void cancel() {
    if (isCompleted() || isClosed() || isCancelled()) {
      throw new DomainException("当前状态的工单不能取消");
    }
    this.status = WorkOrderStatusEnum.CANCELLED.getCode();
  }

  /**
   * 工序报工（行级校验）
   *
   * <p>报工明细由 {@code WorkOrderTracking} 承载，权威数在流水里；行上的合计由报工侧按流水回写。
   * 报工不要求先开工：工序状态按累计良品算，报第一笔就是加工中。数量闸在报工领域服务里按流水算
   * （本聚合不读报工仓储）。
   *
   * @param workOrderOperationId 工单工序Id
   * @param producedQty 本次完成数量
   * @throws DomainException 工单已结束、或报工数据非法时抛出
   */
  public void reportOperation(long workOrderOperationId, BigDecimal producedQty) {
    ensureExecutable();
    requireOperation(workOrderOperationId).report(producedQty);
  }

  /**
   * 撤回报工：只查顺序，减数由报工侧回写做
   *
   * <p>只能倒序冲：后面已经报过工的工序，吃的就是本工序报出来的数，本工序一冲它们的产出就没了依据，所以要 先把后面那道的报工单冲干净。
   *
   * <p>已完工的工单允许撤——工序全部报满会把工单自动推到已完工，撤回正好是它的逆操作；回写把工序状态退下去后，
   * 工单也跟着退回生产中。
   *
   * <p>标了「报工倒冲」的用料不会因为这次撤回自动退料：那张已照批的领料单是另一笔业务，料要还回去就另开一张 退料单，两边各自校验、各自留单据痕迹。
   *
   * @param workOrderOperationId 工单工序Id
   * @throws DomainException 工单未下达/已关闭/已取消、或后续工序已报过工时抛出
   */
  public void unreportOperation(long workOrderOperationId) {
    ensureRevocable("撤回报工");
    WorkOrderOperation operation = requireOperation(workOrderOperationId);
    WorkOrderOperation later = firstLaterProductionReported(operation);
    if (later != null) {
      throw new DomainException("后续工序" + later.getOperationName() + "已经报过工，请先撤回它的报工");
    }
  }

  /**
   * 回写流水汇总的工序合计
   *
   * <p>照 ERPNext 的作业卡提交节奏：良品、报废、工时的权威数在报工单流水里，报工侧在单据生效/作废后按
   * 流水汇总回写这道工序的落库合计，工序状态也在这里按量重算。第一笔报工把已下达的工单带进生产中，
   * 工序全部报满则工单转完工——都是量的结论，不需要人再点开工、完工。
   *
   * @param workOrderOperationId 工单工序Id
   * @param producedQty 流水已完成数量
   * @param moment 报工时刻
   * @throws DomainException 工序不存在时抛出
   */
  public void applyOperationTotals(
      long workOrderOperationId, BigDecimal producedQty, LocalDateTime moment) {
    WorkOrderOperation operation = requireOperation(workOrderOperationId);
    operation.applyOperationTotals(producedQty, availableQtyFor(operation));
    if (isReleased()) {
      start();
    }
    refreshCompletionStatus(moment);
  }

  /**
   * 取工序可投入数量（供报工侧的数量闸使用）
   *
   * @param workOrderOperationId 工单工序Id
   * @return 本工序允许报工的数量上限
   */
  public BigDecimal availableQtyFor(long workOrderOperationId) {
    return availableQtyFor(requireOperation(workOrderOperationId));
  }

  /**
   * 领料：累加工单用料行的已领数量
   *
   * <p>用料行是倒冲行时，已耗跟着一起加——出库即进了物料，两个数是一笔。
   *
   * @param workOrderLineId 工单用料Id
   * @param qty 本次领用数量
   * @throws DomainException 工单已结束、用料行不存在、或净领料累计超过需求数量时抛出
   */
  public void issueMaterial(long workOrderLineId, BigDecimal qty) {
    ensureExecutable();
    requireLine(workOrderLineId).issue(qty);
  }

  /**
   * 退料：累加已退数量
   *
   * @param workOrderLineId 工单用料Id
   * @param qty 本次退料数量
   * @throws DomainException 工单已结束、用料行不存在、或退料超过已领未耗数量时抛出
   */
  public void returnMaterial(long workOrderLineId, BigDecimal qty) {
    ensureExecutable();
    requireLine(workOrderLineId).returnMaterial(qty);
  }

  /**
   * 撤回领料：把用料行的已领数量减回去
   *
   * <p>走的是 {@code ensureRevocable} 而不是 {@code ensureExecutable}：领错料的单子往往在工单已经完工之后
   * 才发现，已完工不该成为撤不回来的理由。
   *
   * @param workOrderLineId 工单用料Id
   * @param qty 本次撤回数量
   * @throws DomainException 工单已关闭/已取消、用料行不存在、或撤回超过已领未耗数量时抛出
   */
  public void unissueMaterial(long workOrderLineId, BigDecimal qty) {
    ensureRevocable("撤回领料");
    requireLine(workOrderLineId).unissue(qty);
  }

  /**
   * 撤回退料：把退料单加上去的已退数量减回去
   *
   * @param workOrderLineId 工单用料Id
   * @param qty 本次撤回数量
   * @throws DomainException 工单已关闭/已取消、用料行不存在、或撤回超过已退数量时抛出
   */
  public void unreturnMaterial(long workOrderLineId, BigDecimal qty) {
    ensureRevocable("撤回退料");
    requireLine(workOrderLineId).unreturn(qty);
  }

  /**
   * 耗用：累加用料行的已耗数量
   *
   * @param workOrderLineId 工单用料Id
   * @param qty 本次耗用数量
   * @throws DomainException 工单已结束、或用料行不存在时抛出
   */
  public void consumeMaterial(long workOrderLineId, BigDecimal qty) {
    ensureExecutable();
    requireLine(workOrderLineId).consume(qty);
  }

  /**
   * 已完工数量
   *
   * <p>不落库：做出来多少本来就是末道已完工工序的完成数，工序行上就有，工单再存一份就得维护两处一致。 末道被撤回、一道都没完工时归零。
   *
   * @return 末道已完工工序的已完成数量数
   */
  public BigDecimal getProducedQty() {
    WorkOrderOperation last = lastCompletedOperation();
    return last == null ? BigDecimal.ZERO : last.getProducedQty();
  }

  /**
   * 按Id查找未删除的工序
   *
   * @param workOrderOperationId 工单工序Id
   * @return 匹配到的工序，找不到返回 {@code null}
   */
  @Nullable
  private WorkOrderOperation findOperation(long workOrderOperationId) {
    return workOrderOperations.stream()
        .filter(o -> Objects.equals(o.getId(), workOrderOperationId) && !o.getIsDeleted())
        .findFirst()
        .orElse(null);
  }

  /**
   * 按顺序查找未删除的工序
   *
   * @param sequence 工序顺序
   * @return 匹配到的工序，找不到返回 {@code null}
   */
  @Nullable
  private WorkOrderOperation findOperationBySequence(int sequence) {
    return workOrderOperations.stream()
        .filter(o -> Objects.equals(o.getSequence(), sequence) && !o.getIsDeleted())
        .findFirst()
        .orElse(null);
  }

  /**
   * 按Id查找未删除的用料行
   *
   * @param workOrderLineId 工单用料Id
   * @return 匹配到的用料行，找不到返回 {@code null}
   */
  @Nullable
  private WorkOrderLine findLine(long workOrderLineId) {
    return workOrderLines.stream()
        .filter(m -> Objects.equals(m.getId(), workOrderLineId) && !m.getIsDeleted())
        .findFirst()
        .orElse(null);
  }

  /**
   * 是否草稿
   *
   * @return true 表示草稿
   */
  public boolean isDraft() {
    return WorkOrderStatusEnum.DRAFT.getCode().equals(this.status);
  }

  /**
   * 是否已下达
   *
   * @return true 表示已下达
   */
  public boolean isReleased() {
    return WorkOrderStatusEnum.RELEASED.getCode().equals(this.status);
  }

  /**
   * 是否生产中
   *
   * @return true 表示生产中
   */
  public boolean isInProgress() {
    return WorkOrderStatusEnum.IN_PROGRESS.getCode().equals(this.status);
  }

  /**
   * 是否已完工
   *
   * @return true 表示已完工
   */
  public boolean isCompleted() {
    return WorkOrderStatusEnum.COMPLETED.getCode().equals(this.status);
  }

  /**
   * 是否已关闭
   *
   * @return true 表示已关闭
   */
  public boolean isClosed() {
    return WorkOrderStatusEnum.CLOSED.getCode().equals(this.status);
  }

  /**
   * 是否已取消
   *
   * @return true 表示已取消
   */
  public boolean isCancelled() {
    return WorkOrderStatusEnum.CANCELLED.getCode().equals(this.status);
  }

  // ==================== 私有：校验与赋值 ====================

  /**
   * 统一的校验与赋值入口（构造器与修改共用）
   *
   * @param sourceType 来源类型编码
   * @param sourceId 来源单据Id（可空）
   * @param sourceLineId 来源单据行Id（可空：库存补货没有行）
   * @param sourceNumber 来源单号（可空）
   * @param materialId 产出SKU的SPU
   * @param variantId 产出SKU
   * @param deliveryTime 交期（可空，承诺的完工时间）
   * @param plannedStartTime 计划开工时间（可空，排产时填）
   * @param qty 计划数量
   * @param unitId 单位Id
   * @param locationId 收货库位Id
   * @param remark 备注（可空）
   * @throws DomainException SPU、SKU、数量、单位、来源类型非法，或开工时间晚于交期时抛出
   */
  private void apply(
      String sourceType,
      @Nullable Long sourceId,
      @Nullable Long sourceLineId,
      @Nullable String sourceNumber,
      long materialId,
      long variantId,
      @Nullable LocalDateTime deliveryTime,
      @Nullable LocalDateTime plannedStartTime,
      BigDecimal qty,
      long unitId,
      long locationId,
      @Nullable String remark) {
    if (materialId <= 0) {
      throw new DomainException("产出SPU不能为空");
    }
    if (variantId <= 0) {
      throw new DomainException("产出SKU不能为空");
    }
    if (qty.signum() <= 0) {
      throw new DomainException("计划数量必须大于0");
    }
    if (unitId <= 0) {
      throw new DomainException("单位不能为空");
    }
    String trimmedSourceType = StringUtils.trimToNull(sourceType);
    if (trimmedSourceType == null) {
      throw new DomainException("来源类型不能为空");
    }
    if (plannedStartTime != null && deliveryTime != null && plannedStartTime.isAfter(deliveryTime)) {
      throw new DomainException("开工时间晚于交期，这张单排不进去");
    }
    this.sourceType = EnumUtils.getCode(WorkOrderSourceTypeEnum.class, trimmedSourceType, "来源类型");

    this.materialId = materialId;
    this.variantId = variantId;
    this.qty = qty;
    this.unitId = unitId;
    this.sourceId = sourceId;
    this.sourceLineId = sourceLineId;
    this.sourceNumber = sourceNumber;
    this.deliveryTime = deliveryTime;
    this.plannedStartTime = plannedStartTime;
    if (locationId <= 0) {
      throw new DomainException("收货库位不能为空");
    }
    this.locationId = locationId;
    this.remark = remark;
  }

  /**
   * 校验工单处于可执行状态
   *
   * @throws DomainException 草稿、已完工、已关闭或已取消时抛出
   */
  private void ensureExecutable() {
    if (isDraft()) {
      throw new DomainException("工单未下达，不能报工");
    }
    if (isCompleted() || isClosed() || isCancelled()) {
      throw new DomainException("工单已结束，不能再报工");
    }
  }

  /**
   * 校验工单还允许把已经发生的业务撤回去
   *
   * <p>领料、退料、报工的作废与撤回都走这一道：单据那边走哪条路不影响工单侧，都是把当初加上去的数减回去。
   *
   * <p>这套判断比 {@code ensureExecutable} 宽：末道工序报满会把工单自动推到已完工，而撤销正是它的逆操作，
   * 已完工不能成为撤不回来的理由。未下达与已关闭/已取消才是真闸门——单据都已经落定，再改数就没人对得上账了。
   *
   * @param action 动作名称，用于报错文案
   * @throws DomainException 工单未下达或已关闭/已取消时抛出
   */
  private void ensureRevocable(String action) {
    if (isDraft()) {
      throw new DomainException("工单未下达，不能" + action);
    }
    if (isClosed() || isCancelled()) {
      throw new DomainException("工单已关闭或已取消，不能" + action);
    }
  }

  /**
   * 取工序，不存在则抛异常
   *
   * @param workOrderOperationId 工单工序Id
   * @return 工单工序
   * @throws DomainException 工序不存在时抛出
   */
  private WorkOrderOperation requireOperation(long workOrderOperationId) {
    WorkOrderOperation operation = findOperation(workOrderOperationId);
    if (operation == null) {
      throw new DomainException("工单工序不存在!");
    }
    return operation;
  }

  /**
   * 取用料行，不存在则抛异常
   *
   * @param workOrderLineId 工单用料Id
   * @return 工单用料
   * @throws DomainException 用料行不存在时抛出
   */
  private WorkOrderLine requireLine(long workOrderLineId) {
    WorkOrderLine line = findLine(workOrderLineId);
    if (line == null) {
      throw new DomainException("用料行不存在!");
    }
    return line;
  }

  /**
   * 取本工序之后第一道已经报过工的工序
   *
   * <p>倒序拦的是「后面那道工序已经用掉你报出来的数」，判据取累计报工数而不是工序状态：撤回会把已完工的
   * 工序退回加工中，若按状态拦，链子冲到第二道就再也冲不动——后面那道明明一张报工单都不剩，却被自己的 「加工中」挡住。
   *
   * @param operation 本工序
   * @return 已经报过工的后续工序，没有时返回 {@code null}
   */
  @Nullable
  private WorkOrderOperation firstLaterProductionReported(WorkOrderOperation operation) {
    return orderedOperations().stream()
        .filter(o -> o.getSequence() > operation.getSequence())
        .filter(WorkOrderOperation::hasReported)
        .findFirst()
        .orElse(null);
  }

  /**
   * 按顺序排列未删除的工序
   *
   * @return 有序工序列表
   */
  private List<WorkOrderOperation> orderedOperations() {
    return workOrderOperations.stream()
        .filter(o -> !o.getIsDeleted())
        .sorted(Comparator.comparing(WorkOrderOperation::getSequence))
        .toList();
  }

  /**
   * 按工序收尾情况刷新工单状态
   *
   * <p>两个方向都在这里：所有工序都报满了就把工单推到已完工并落完工时刻；某道工序被撤回、不再全部报满，
   * 已完工的工单退回生产中，完工时刻一并清空。只在生产中的工单上动——已关闭、已取消的工单不该被报工带回来。
   *
   * @param moment 报工时刻
   */
  private void refreshCompletionStatus(LocalDateTime moment) {
    if (!isInProgress() && !isCompleted()) {
      return;
    }
    if (allOperationsFinished()) {
      if (!isCompleted()) {
        this.status = WorkOrderStatusEnum.COMPLETED.getCode();
        this.completedTime = moment;
      }
      return;
    }
    if (isCompleted()) {
      this.status = WorkOrderStatusEnum.IN_PROGRESS.getCode();
      this.completedTime = null;
    }
  }

  /**
   * 是否所有工序都已完工
   *
   * <p>逐道判而不是只看末道：末道能完工说明前面都完了，但判据直接说「全部完工」，读代码的人不必再推一遍顺序。
   *
   * @return true 表示没有工序挂在未开始或加工中
   */
  private boolean allOperationsFinished() {
    return workOrderOperations.stream()
        .filter(o -> !o.getIsDeleted())
        .allMatch(WorkOrderOperation::isCompleted);
  }

  /**
   * 是否已达数量口径：末道良品加全流程报废不小于计划数量
   *
   * @return true 表示已达成
   */
  private boolean isQtySettled() {
    return getProducedQty().compareTo(this.qty) >= 0;
  }

  /**
   * 计算工序可投入数量
   *
   * <p>首道工序取工单计划数量；其余取紧邻上游工序的已完成数量。
   *
   * @param operation 工单工序
   * @return 本工序允许报工的数量上限
   */
  private BigDecimal availableQtyFor(WorkOrderOperation operation) {
    WorkOrderOperation predecessor = lastEffectivePredecessor(operation);
    return predecessor == null ? this.qty : predecessor.getProducedQty();
  }

  /**
   * 取紧邻在本工序之前的上游工序
   *
   * @param operation 工单工序
   * @return 上游工序，本工序即首道时返回 {@code null}
   */
  @Nullable
  private WorkOrderOperation lastEffectivePredecessor(WorkOrderOperation operation) {
    return orderedOperations().stream()
        .filter(o -> !Objects.equals(o.getId(), operation.getId()))
        .filter(o -> o.getSequence() < operation.getSequence())
        .reduce((first, second) -> second)
        .orElse(null);
  }

  /**
   * 取顺序最靠后的已完工工序
   *
   * <p>工单的完工数应当以最后一道真正干完的工序为准。
   *
   * @return 末道已完工工序，没有已完工工序时返回 {@code null}
   */
  @Nullable
  private WorkOrderOperation lastCompletedOperation() {
    return workOrderOperations.stream()
        .filter(o -> !o.getIsDeleted())
        .filter(WorkOrderOperation::isCompleted)
        .max(Comparator.comparing(WorkOrderOperation::getSequence))
        .orElse(null);
  }
}
