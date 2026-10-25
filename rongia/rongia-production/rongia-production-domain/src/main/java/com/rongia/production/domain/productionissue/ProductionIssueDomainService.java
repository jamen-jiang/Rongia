package com.rongia.production.domain.productionissue;

import com.rongia.production.domain.workorder.WorkOrder;
import com.rongia.production.domain.workorder.WorkOrderRepository;
import com.rongia.shared.domain.exception.DomainException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 生产领料单领域服务
 *
 * <p>发料要同时动两个聚合：生产领料单自己把核定数量落定，工单用料行按核定数量累加已领。这是跨聚合的一致性动作，
 * 所以放在领域服务里编排，而不是让生产领料单去反向修改工单。
 *
 * <p>库存那一半不在这里：本模块不依赖库存模块，三个时点由应用层在同一个事务里连着调库存服务——**批准**时
 * {@code reserve}（可用量减掉这笔），**发料过账**时先 {@code unreserve} 再 {@code outbound}（先退自己的占用，
 * 免得出库时被自己的预留挡住），**改小批准量或作废**时按差额 {@code unreserve}。定位一律按行上的仓库与库位。
 *
 * <p>撤销只有一条路：草稿删掉重开，一分货没动。已发料的不在生产领料单上翻状态——料真出过仓，冲它是在库存那边
 * 照原库存记录开一张红字单；红字单过账后由应用层调本服务的 {@code reverse}，把工单用料的已领数量按冲掉的量退回去。
 */
@NullMarked
public class ProductionIssueDomainService {

  /** 生产领料单工厂 */
  private final ProductionIssueFactory productionIssueFactory;

  /** 生产领料单仓储 */
  private final ProductionIssueRepository productionIssueRepository;

  /** 工单仓储 */
  private final WorkOrderRepository workOrderRepository;

  /**
   * 构造器注入
   *
   * @param productionIssueFactory 生产领料单工厂
   * @param productionIssueRepository 生产领料单仓储
   * @param workOrderRepository 工单仓储
   */
  public ProductionIssueDomainService(
      ProductionIssueFactory productionIssueFactory,
      ProductionIssueRepository productionIssueRepository,
      WorkOrderRepository workOrderRepository) {
    this.productionIssueFactory = productionIssueFactory;
    this.productionIssueRepository = productionIssueRepository;
    this.workOrderRepository = workOrderRepository;
  }

  /**
   * 创建生产领料单（草稿）
   *
   * @param workOrderId 工单Id
   * @param workOrderNumber 工单号（冗余，便于展示）
   * @param operatorId 经办人Id（可空）
   * @param remark 备注（可空）
   * @param lines 领料行数据
   * @return 生产领料单聚合
   */
  public ProductionIssue create(
      long workOrderId,
      String workOrderNumber,
      @Nullable Long operatorId,
      @Nullable String remark,
      List<ProductionIssueLineData> lines) {
    // 传空串：单号交给 OrderAddedEvent 的监听器分配
    ProductionIssue model =
        productionIssueFactory.create(
            workOrderId, workOrderNumber, operatorId, remark, "");
    for (ProductionIssueLineData data : lines) {
      model.addLine(
          data.workOrderMaterialId(),
          data.materialId(),
          data.variantId(),
          data.appliedQty(),
          data.unitId(),
          data.warehouseId(),
          data.remark());
    }
    return model;
  }

  /**
   * 修改单据头（仅草稿可改）
   *
   * @param id 生产领料单Id
   * @param workOrderId 工单Id
   * @param workOrderNumber 工单号（冗余，便于展示）
   * @param operatorId 经办人Id（可空）
   * @param remark 备注（可空）
   * @return 生产领料单聚合
   * @throws DomainException 生产领料单不存在时抛出
   */
  public ProductionIssue modify(
      long id,
      long workOrderId,
      String workOrderNumber,
      @Nullable Long operatorId,
      @Nullable String remark) {
    ProductionIssue model = getExist(id);
    model.modify(workOrderId, workOrderNumber, operatorId, remark);
    return model;
  }

  /**
   * 添加领料行（仅草稿可加）
   *
   * @param id 生产领料单Id
   * @param data 领料行数据
   * @return 生产领料单聚合
   * @throws DomainException 生产领料单不存在时抛出
   */
  public ProductionIssue addLine(long id, ProductionIssueLineData data) {
    ProductionIssue model = getExist(id);
    model.addLine(
        data.workOrderMaterialId(),
        data.materialId(),
        data.variantId(),
        data.appliedQty(),
        data.unitId(),
        data.warehouseId(),
        data.remark());
    return model;
  }

  /**
   * 删除领料行（仅草稿可删）
   *
   * @param id 生产领料单Id
   * @param workOrderMaterialId 工单用料行Id
   * @return 生产领料单聚合
   * @throws DomainException 生产领料单不存在时抛出
   */
  public ProductionIssue deleteLine(long id, long workOrderMaterialId) {
    ProductionIssue model = getExist(id);
    model.deleteLine(workOrderMaterialId);
    return model;
  }

  /**
   * 发料：核定数量落到行上、计入工单用料，单据转入已发料
   *
   * <p>审核与发料合成一步。先让工单侧校验通过再改单据状态——反过来的话，工单侧一抛异常就会留下一张「已发料但没
   * 计入工单」的生产领料单。超领上限由工单用料行自己那三个数量说了算，这里不另算一份。
   *
   * <p>货从行上的仓库出去由应用层在同一事务里调库存服务完成，本方法只管单据与工单这一半。
   *
   * @param id 生产领料单Id
   * @param approvals 逐行核定清单，没点到的行按申请数照批
   * @param time 发料时间
   * @return 生产领料单聚合
   * @throws DomainException 生产领料单或工单不存在、用料行不属于本单、单内没有行、或工单侧数量校验不过时抛出
   */
  public ProductionIssue post(
      long id, List<ProductionIssueApprovalData> approvals, LocalDateTime time) {
    ProductionIssue model = getExist(id);
    WorkOrder workOrder = requireWorkOrder(model.getWorkOrderId());
    model.approve(approvals);
    model.post(time);
    for (ProductionIssueLine line : model.getProductionIssueLines()) {
      workOrder.issueMaterial(line.getWorkOrderMaterialId(), line.getApprovedQty());
    }
    return model;
  }

  /**
   * 红冲：把红字库存单冲掉的量从工单用料行减回去，单据停在已发料
   *
   * <p>只走已发料的单子。红字单开在库存那边，本方法只补工单这一半：把已领数量按冲掉的量减回去。单据不翻状态——
   * 它确实发过料，货是库存那边倒回来的，翻状态等于抹掉这段事实；「被冲过没有」去库存那边查指着它的红字单。
   *
   * <p>可以分次冲，每次只传这次冲掉的几行几量。累计上限由库存那边管（同一原库存记录累计冲量不超过原量），这里
   * 只管单次不超本行核定数，以及工单用料行自己的上限（手工行不超已领未耗，倒冲行不超已耗并同时把已耗减回去）。
   *
   * @param id 生产领料单Id
   * @param lines 本次冲回的行与数量（数量为正）
   * @return 生产领料单聚合
   * @throws DomainException 生产领料单或工单不存在、单据不是已发料状态、行不在本单内或重复、数量非法、或工单侧校验不过时抛出
   */
  public ProductionIssue reverse(long id, List<ProductionIssueReversalData> lines) {
    ProductionIssue model = getExist(id);
    if (!model.isPosted()) {
      throw new DomainException("生产领料单" + model.getNumber() + "还没发料，删掉重开就行，不用红冲");
    }
    if (lines.isEmpty()) {
      throw new DomainException("红冲得有行：说清冲哪几行、各冲多少");
    }
    WorkOrder workOrder = requireWorkOrder(model.getWorkOrderId());
    Set<Long> seen = new HashSet<>();
    for (ProductionIssueReversalData data : lines) {
      if (!seen.add(data.workOrderMaterialId())) {
        throw new DomainException("工单用料" + data.workOrderMaterialId() + "重复冲回!");
      }
      ProductionIssueLine line = requireLine(model, data.workOrderMaterialId());
      BigDecimal qty = data.qty();
      if (qty.signum() <= 0) {
        throw new DomainException("冲回数量必须大于0");
      }
      if (qty.compareTo(line.getApprovedQty()) > 0) {
        throw new DomainException(
            "物料"
                + line.getVariantId()
                + "冲回数量"
                + qty
                + "不能大于本单核定数量"
                + line.getApprovedQty());
      }
      workOrder.unissueMaterial(line.getWorkOrderMaterialId(), qty);
    }
    return model;
  }

  // ==================== 私有 ====================

  /**
   * 按工单用料行Id取本单里的领料行，不存在则抛异常
   *
   * @param model 生产领料单聚合
   * @param workOrderMaterialId 工单用料行Id
   * @return 领料行
   * @throws DomainException 该用料行不在本单内时抛出
   */
  private ProductionIssueLine requireLine(ProductionIssue model, long workOrderMaterialId) {
    for (ProductionIssueLine line : model.getProductionIssueLines()) {
      if (Objects.equals(line.getWorkOrderMaterialId(), workOrderMaterialId)
          && !line.getIsDeleted()) {
        return line;
      }
    }
    throw new DomainException("工单用料" + workOrderMaterialId + "不在生产领料单" + model.getNumber() + "中!");
  }

  /**
   * 取生产领料单，不存在则抛异常
   *
   * @param id 生产领料单Id
   * @return 生产领料单聚合
   * @throws DomainException 生产领料单不存在时抛出
   */
  private ProductionIssue getExist(long id) {
    ProductionIssue model = productionIssueRepository.get(id);
    if (model == null) {
      throw new DomainException("生产领料单" + id + "不存在!");
    }
    return model;
  }

  /**
   * 取工单，不存在则抛异常
   *
   * @param workOrderId 工单Id
   * @return 工单聚合
   * @throws DomainException 工单不存在时抛出
   */
  private WorkOrder requireWorkOrder(long workOrderId) {
    WorkOrder workOrder = workOrderRepository.get(workOrderId);
    if (workOrder == null) {
      throw new DomainException("工单" + workOrderId + "不存在!");
    }
    return workOrder;
  }
}
