package com.rongia.production.domain.workorder;

import com.rongia.production.domain.bom.Bom;
import com.rongia.production.domain.bom.BomLine;
import com.rongia.production.domain.bom.BomRepository;
import com.rongia.production.domain.bom.BomTypeEnum;
import com.rongia.production.domain.operation.Operation;
import com.rongia.production.domain.operation.OperationRepository;
import com.rongia.production.domain.routing.Routing;
import com.rongia.production.domain.routing.RoutingOperation;
import com.rongia.production.domain.routing.RoutingRepository;
import com.rongia.production.domain.workcenter.WorkCenter;
import com.rongia.production.domain.workcenter.WorkCenterRepository;
import com.rongia.shared.domain.exception.DomainException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 工单领域服务
 *
 * <p>负责跨聚合编排：下达工单时把 BOM 展开成用料、把工艺路线快照成工序。这两件事都需要读别的聚合
 * （{@code Bom} / {@code Routing} / {@code Operation} / {@code WorkCenter}），所以放在领域服务而不是
 * {@code WorkOrder} 自己身上——聚合根只能管自己的不变量。
 *
 * <p>下达遵循「先全量校验、再一次性装配」：校验不过就抛异常，工单还停在草稿上，不会留下半套工序或用料。
 */
@NullMarked
public class WorkOrderDomainService {

  /** 工单工厂 */
  private final WorkOrderFactory workOrderFactory;

  /** 工单仓储 */
  private final WorkOrderRepository workOrderRepository;

  /** BOM 仓储 */
  private final BomRepository bomRepository;

  /** 工艺路线仓储 */
  private final RoutingRepository routingRepository;

  /** 工序仓储 */
  private final OperationRepository operationRepository;

  /** 工作中心仓储 */
  private final WorkCenterRepository workCenterRepository;

  /**
   * 构造器注入
   *
   * @param workOrderFactory 工单工厂
   * @param workOrderRepository 工单仓储
   * @param bomRepository BOM 仓储
   * @param routingRepository 工艺路线仓储
   * @param operationRepository 工序仓储
   * @param workCenterRepository 工作中心仓储
   */
  public WorkOrderDomainService(
      WorkOrderFactory workOrderFactory,
      WorkOrderRepository workOrderRepository,
      BomRepository bomRepository,
      RoutingRepository routingRepository,
      OperationRepository operationRepository,
      WorkCenterRepository workCenterRepository) {
    this.workOrderFactory = workOrderFactory;
    this.workOrderRepository = workOrderRepository;
    this.bomRepository = bomRepository;
    this.routingRepository = routingRepository;
    this.operationRepository = operationRepository;
    this.workCenterRepository = workCenterRepository;
  }

  /**
   * 创建工单（草稿）
   *
   * @param sourceType 来源类型编码
   * @param sourceId 来源单据Id（可空，库存补货时为空）
   * @param sourceNumber 来源单号（可空，冗余存储便于追溯与展示）
   * @param bomId 指定BOM（可空，为空时下达自动取生效版本）
   * @param routingId 指定工艺路线（可空，为空时下达自动取生效版本）
   * @param materialId 产出SPU
   * @param variantId 产出SKU
   * @param deliveryTime 交期（可空，承诺的完工时间，生产侧只读不写）
   * @param startTime 开工时间（可空，排产时填计划，照 Odoo 的 date_start 语义）
   * @param unitId 单位Id
   * @param pickingTypeId 操作类型Id
   * @param srcWarehouseId 发料仓库Id
   * @param destWarehouseId 成品收货仓库Id
   * @param finalWarehouseId 最终仓库Id（可空）
   * @param productionWarehouseId 生产仓库Id（可空）

   * @param remark 备注（可空）
   * @return 工单聚合
   */
  public WorkOrder create(
      String sourceType,
      @Nullable Long sourceId,
      @Nullable String sourceNumber,
      String priority,
      @Nullable Long bomId,
      @Nullable Long routingId,
      long materialId,
      long variantId,
      @Nullable LocalDateTime deliveryTime,
      @Nullable LocalDateTime startTime,
      BigDecimal qty,
      long unitId,
      long pickingTypeId,
      long srcWarehouseId,
      long destWarehouseId,
      @Nullable Long finalWarehouseId,
      @Nullable Long productionWarehouseId,
      @Nullable String remark) {
    // 传空串：单号交给 OrderAddedEvent 的监听器分配
    return workOrderFactory.create(
        sourceType,
        sourceId,
        sourceNumber,
        priority,
        bomId,
        routingId,
        materialId,
        variantId,
        deliveryTime,
        startTime,
        qty,
        unitId,
        pickingTypeId,
        srcWarehouseId,
        destWarehouseId,
        finalWarehouseId,
        productionWarehouseId,
        remark,
        "");
  }

  /**
   * 修改工单（仅草稿可改）
   *
   * @param id 工单Id
   * @param sourceType 来源类型编码
   * @param sourceId 来源单据Id（可空，库存补货时为空）
   * @param sourceNumber 来源单号（可空，冗余存储便于追溯与展示）
   * @param bomId 指定BOM（可空，为空时下达自动取生效版本）
   * @param routingId 指定工艺路线（可空，为空时下达自动取生效版本）
   * @param materialId 产出SPU
   * @param variantId 产出SKU
   * @param deliveryTime 交期（可空，承诺的完工时间，生产侧只读不写）
   * @param qty 计划数量
   * @param unitId 单位Id
   * @param pickingTypeId 操作类型Id
   * @param srcWarehouseId 发料仓库Id
   * @param destWarehouseId 成品收货仓库Id
   * @param finalWarehouseId 最终仓库Id（可空）
   * @param productionWarehouseId 生产仓库Id（可空）

   * @param remark 备注（可空）
   * @return 工单聚合
   * @throws DomainException 工单不存在、非草稿状态时抛出
   */
  public WorkOrder modify(
      long id,
      String sourceType,
      @Nullable Long sourceId,
      @Nullable String sourceNumber,
      String priority,
      @Nullable Long bomId,
      @Nullable Long routingId,
      long materialId,
      long variantId,
      @Nullable LocalDateTime deliveryTime,
      @Nullable LocalDateTime startTime,
      BigDecimal qty,
      long unitId,
      long pickingTypeId,
      long srcWarehouseId,
      long destWarehouseId,
      @Nullable Long finalWarehouseId,
      @Nullable Long productionWarehouseId,
      @Nullable String remark) {
    WorkOrder model = getExist(id);
    model.modify(
        sourceType,
        sourceId,
        sourceNumber,
        priority,
        bomId,
        routingId,
        materialId,
        variantId,
        deliveryTime,
        startTime,
        qty,
        unitId,
        pickingTypeId,
        srcWarehouseId,
        destWarehouseId,
        finalWarehouseId,
        productionWarehouseId,
        remark);
    return model;
  }

  /**
   * 下达工单：按 BOM 展开用料、按工艺路线快照工序
   *
   * <p>BOM 与路线都遵循「工单上指定优先，没指定才取生效版本」，且都按下达时点取版本：补一张上个月的单，
   * 拿到的是上个月那一版配方与工艺，不是今天最新版。路线挂在物料系列上，工单没指定路线时，取该物料在
   * 下达时点生效的主路线。
   *
   * @param id 工单Id
   * @param routingId 本次下达使用的工艺路线（可空，为空时取工单上指定的，再为空取物料的主路线）
   * @return 工单聚合
   * @throws DomainException 工单不存在、物料没有生效的主路线、路线在该时点不生效、引用的工序、工作中心或
   *     费率卡不可用时抛出
   */
  public WorkOrder release(long id, @Nullable Long routingId) {
    WorkOrder model = getExist(id);

    // 1. 定 BOM：工单指定优先，其次取该父件未禁用的 BOM
    Long resolvedBomId = model.getBomId();
    Bom bom =
        resolvedBomId == null
            ? bomRepository.getEnabledByVariantId(model.getMaterialId(), model.getVariantId())
            : bomRepository.get(resolvedBomId);
    if (resolvedBomId != null && bom == null) {
      throw new DomainException("BOM" + resolvedBomId + "不存在!");
    }
    if (bom != null && Boolean.TRUE.equals(bom.getDisabled())) {
      throw new DomainException("BOM" + bom.getId() + "已禁用，不能用于下达工单");
    }

    // 2. 定路线：本次传入优先，其次用工单上指定的，再次用 BOM 上指定的
    Long resolvedRoutingId = routingId == null ? model.getRoutingId() : routingId;
    if (resolvedRoutingId == null && bom != null) {
      resolvedRoutingId = bom.getRoutingId();
    }
    if (resolvedRoutingId == null) {
      throw new DomainException("工单" + model.getNumber() + "没有可用的工艺路线，不能下达");
    }
    Routing routing = routingRepository.get(resolvedRoutingId);
    if (routing == null) {
      throw new DomainException("工艺路线" + resolvedRoutingId + "不存在!");
    }
    if (Boolean.TRUE.equals(routing.getDisabled())) {
      throw new DomainException(
          "工艺路线" + routing.getCode() + "版本" + routing.getVersion() + "已禁用，不能用于下达工单");
    }

    // 3. 先全量校验外部引用，确认能装配再动手
    List<OperationSnapshot> snapshots = resolveOperations(routing);
    List<MaterialRequirement> requirements = resolveMaterials(model.getQty(), bom);

    // 4. 一次性装配并固化
    for (OperationSnapshot snapshot : snapshots) {
      model.addOperation(
          snapshot.operationId(),
          snapshot.operationName(),
          snapshot.workCenterId(),
          snapshot.workCenterName(),
          snapshot.cycleTime(),
          snapshot.sequence());
    }
    for (MaterialRequirement requirement : requirements) {
      model.addMaterial(
          requirement.bomLineId(),
          requirement.materialId(),
          requirement.variantId(),
          requirement.routingOperationId(),
          requirement.requiredQty(),
          requirement.unitId());
    }
    model.release(bom == null ? null : bom.getId(), routing.getId());
    scheduleOperations(model);
    return model;
  }

  /**
   * 排产：按工序周期链正向铺计划时间
   *
   * <p>工单头上有开工时间就从它起铺，没有则不铺（排产由车间之后手工补）。铺法照 ERPNext 的
   * planned_start_time / planned_end_time：首道开工 = 工单开工时间，每道完工 = 开工 + 周期时间 × 计划数量，
   * 下一道开工 = 上一道完工——线性流水，不排并行。
   *
   * @param model 已下达的工单
   */
  private void scheduleOperations(WorkOrder model) {
    LocalDateTime cursor = model.getStartTime();
    if (cursor == null) {
      return;
    }
    for (WorkOrderOperation operation : model.getWorkOrderOperations()) {
      if (operation.isSkipped()) {
        continue;
      }
      LocalDateTime end =
          cursor.plusMinutes(
              operation.estimatedMinutes(model.getQty()).longValue());
      operation.schedule(cursor, end);
      cursor = end;
    }
    model.fillEndTime(cursor);
  }

  /**
   * 取消工单
   *
   * @param id 工单Id
   * @return 工单聚合
   * @throws DomainException 工单不存在、或当前状态不允许取消时抛出
   */
  public WorkOrder cancel(long id) {
    WorkOrder model = getExist(id);
    model.cancel();
    return model;
  }

  // ==================== 私有：展开与快照 ====================

  /**
   * 取工单，不存在则抛异常
   *
   * @param id 工单Id
   * @return 工单聚合
   * @throws DomainException 工单不存在时抛出
   */
  private WorkOrder getExist(long id) {
    WorkOrder model = workOrderRepository.get(id);
    if (model == null) {
      throw new DomainException("工单" + id + "不存在!");
    }
    return model;
  }

  /**
   * 解析工艺路线，得到可直接快照成工单工序的行
   *
   * <p>工序名、工作中心名、工时与计算方式全部冗余存一份：路线或工序主数据后续改名改配置，
   * 在制工单不受影响。这里先跑完整条路线再返回，任何一行引用失效都会在任何工序进工单之前抛出来。
   *
   * @param routing 工艺路线
   * @return 有序、可装配的工序快照列表
   * @throws DomainException 引用的工序、工作中心或费率卡不可用、或工序顺序重复时抛出
   */
  private List<OperationSnapshot> resolveOperations(Routing routing) {
    List<RoutingOperation> items =
        routing.getRoutingOperations().stream()
            .filter(o -> !o.getIsDeleted())
            .sorted(Comparator.comparing(RoutingOperation::getSequence))
            .toList();
    Set<Integer> sequences = new HashSet<>();
    List<OperationSnapshot> result = new ArrayList<>(items.size());
    for (RoutingOperation item : items) {
      if (!sequences.add(item.getSequence())) {
        throw new DomainException(
            "工艺路线" + routing.getCode() + "的工序顺序" + item.getSequence() + "重复!");
      }
      Operation operation = operationRepository.get(item.getOperationId());
      if (operation == null) {
        throw new DomainException("工序" + item.getOperationId() + "不存在!");
      }
      WorkCenter workCenter = workCenterRepository.get(item.getWorkCenterId());
      if (workCenter == null) {
        throw new DomainException("工作中心" + item.getWorkCenterId() + "不存在!");
      }
      result.add(
          new OperationSnapshot(
              operation.getId(),
              operation.getName(),
              workCenter.getId(),
              workCenter.getName(),
              item.getCycleTime(),
              item.getSequence()));
    }
    return List.copyOf(result);
  }

  /**
   * 展开 BOM，得到可直接装配成工单用料的行
   *
   * <p>遇到 {@code type = Phantom}（套件）的子件要穿透：套件本身不单独下工单，它只是一组子件的打包，
   * 所以把它的 BOM 行按折算后的数量接到本单用料里，套件本身不占一行。普通自制件不穿透——它有自己的一张工单，
   * 料发给它，不该在这张单上重复要。是否穿透只看子件自己的 BOM 是不是套件，不看它是不是自制件：真领不到的时候
   * 由库存那边拦，出库时「该键没有库存行」就是答案。
   *
   * <p>展开按层递归，同一子件可能在多条套件路径下出现（毛坯同时用在两个套件里），最后按 SKU 合并数量，
   * 否则领料时会看到两条同 SKU 的用料、分不清该领哪条。合并要求单位一致——BOM 行各自带单位，不做换算时
   * 两个单位相加没有意义。
   *
   * <p>物料没有 BOM 时返回空列表——纯加工类工单不消耗物料，允许下达。
   *
   * <p>需求量在这里一次算好、快照到用料行：之后领料或退料只做工单上的加减，不再回头读 BOM，BOM 之后改版才不会
   * 把在制工单的领料量改掉。
   *
   * @param qty 工单计划数量
   * @param bom 展开所用的 BOM（可空，为空表示物料没有 BOM）
   * @return 可装配的用料需求列表（按 SKU 去重）
   * @throws DomainException BOM 存在循环引用、或同一子件展开后单位不一致时抛出
   */
  List<MaterialRequirement> resolveMaterials(BigDecimal qty, @Nullable Bom bom) {
    if (bom == null) {
      return List.of();
    }
    Map<Long, MaterialRequirement> merged = new LinkedHashMap<>();
    Set<Long> path = new HashSet<>();
    path.add(bom.getVariantId());
    expand(bom, qty, path, merged);
    return List.copyOf(merged.values());
  }

  /**
   * 递归展开一层 BOM，把用料写进累加表
   *
   * @param bom 当前这一层的 BOM
   * @param parentQty 这一层 BOM 的产出物料需要多少个
   * @param path 当前展开路径上的 SKU（用于发现循环引用）
   * @param result 按 SKU 累加的结果表
   * @throws DomainException BOM 存在循环引用、或同一子件单位不一致时抛出
   */
  private void expand(
      Bom bom, BigDecimal parentQty, Set<Long> path, Map<Long, MaterialRequirement> result) {
    for (BomLine line : bom.getBomLines()) {
      if (line.getIsDeleted()) {
        continue;
      }
      BigDecimal required = line.requiredQty(parentQty, bom.getQty());

      // 子件若是套件，穿透到它的下一层；套件自己不成行
      Bom childBom =
          bomRepository.getEnabledByVariantId(line.getMaterialId(), line.getVariantId());
      if (childBom != null && BomTypeEnum.PHANTOM.getCode().equals(childBom.getType())) {
        if (!path.add(line.getVariantId())) {
          throw new DomainException(
              "BOM 存在循环引用：套件" + line.getVariantId() + "又回到了自身所在的层级");
        }
        expand(childBom, required, path, result);
        path.remove(line.getVariantId());
        continue;
      }

      merge(
          result,
          new MaterialRequirement(
              line.getId(),
              line.getMaterialId(),
              line.getVariantId(),
              line.getRoutingOperationId(),
              required,
              line.getUnitId()));
    }
  }

  /**
   * 把一条用料需求累加进结果表
   *
   * <p>同一 SKU 再次出现就把数量加上去，来源行 Id 保留第一次那条；单位不同则拒绝合并。
   *
   * @param result 按 SKU 累加的结果表
   * @param item 待累加的用料需求
   * @throws DomainException 同一子件两次出现但单位不同时抛出
   */
  private void merge(Map<Long, MaterialRequirement> result, MaterialRequirement item) {
    MaterialRequirement existing = result.get(item.variantId());
    if (existing == null) {
      result.put(item.variantId(), item);
      return;
    }
    if (!existing.unitId().equals(item.unitId())) {
      throw new DomainException("子件" + item.variantId() + "展开后出现两个单位，无法合并");
    }
    result.put(
        item.variantId(),
        new MaterialRequirement(
            existing.bomLineId() == null ? item.bomLineId() : existing.bomLineId(),
            existing.materialId(),
            existing.variantId(),
            existing.routingOperationId() == null
                ? item.routingOperationId()
                : existing.routingOperationId(),
            existing.requiredQty().add(item.requiredQty()),
            existing.unitId()));
  }

  /** 待装配的工序快照，字段与 {@link WorkOrder#addOperation} 一一对应 */
  private record OperationSnapshot(
      Long operationId,
      String operationName,
      Long workCenterId,
      String workCenterName,
      BigDecimal cycleTime,
      Integer sequence) {}

  /** 待装配的用料需求，字段与 {@link WorkOrder#addMaterial} 一一对应 */
  record MaterialRequirement(
      @Nullable Long bomLineId,
      Long materialId,
      Long variantId,
      @Nullable Long routingOperationId,
      BigDecimal requiredQty,
      Long unitId) {}
}
