package com.rongia.production.domain.productionreturn;

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
 * 生产退料单领域服务
 *
 * <p>审核要同时动两个聚合：退料单自己把核定数量落定，工单用料行按核定数量累加已退。这是跨聚合的一致性动作，
 * 所以放在领域服务里编排，而不是让退料单去反向修改工单。
 *
 * <p>库存那一半不在这里：本模块不依赖库存模块，回仓入库由应用层调 {@code StockDomainService} 完成，按行上的
 * 仓库与库位把货加回去。
 *
 * <p>撤销只有一条路：草稿删掉重开，一分货没动。已退料的不在退料单上翻状态——货真回过仓库，冲它是在库存那边
 * 照原库存记录开一张红字单；红字单过账后由应用层调本服务的 {@code reverse}，把工单用料的已退数量按冲掉的量减回去。
 */
@NullMarked
public class ProductionReturnDomainService {

  /** 生产退料单工厂 */
  private final ProductionReturnFactory productionReturnFactory;

  /** 生产退料单仓储 */
  private final ProductionReturnRepository productionReturnRepository;

  /** 工单仓储 */
  private final WorkOrderRepository workOrderRepository;

  /**
   * 构造器注入
   *
   * @param productionReturnFactory 生产退料单工厂
   * @param productionReturnRepository 生产退料单仓储
   * @param workOrderRepository 工单仓储
   */
  public ProductionReturnDomainService(
      ProductionReturnFactory productionReturnFactory,
      ProductionReturnRepository productionReturnRepository,
      WorkOrderRepository workOrderRepository) {
    this.productionReturnFactory = productionReturnFactory;
    this.productionReturnRepository = productionReturnRepository;
    this.workOrderRepository = workOrderRepository;
  }

  /**
   * 创建退料单（草稿）
   *
   * @param workOrderId 工单Id
   * @param workOrderNumber 工单号（冗余，便于展示）
   * @param operatorId 经办人Id（可空）
   * @param remark 备注（可空）
   * @param lines 退料行数据
   * @return 生产退料单聚合
   */
  public ProductionReturn create(
      long workOrderId,
      String workOrderNumber,
      @Nullable Long operatorId,
      @Nullable String remark,
      List<ProductionReturnLineData> lines) {
    // 传空串：单号交给 OrderAddedEvent 的监听器分配
    ProductionReturn model =
        productionReturnFactory.create(workOrderId, workOrderNumber, operatorId, remark, "");
    for (ProductionReturnLineData data : lines) {
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
   * @param id 退料单Id
   * @param workOrderId 工单Id
   * @param workOrderNumber 工单号（冗余，便于展示）
   * @param operatorId 经办人Id（可空）
   * @param remark 备注（可空）
   * @return 生产退料单聚合
   * @throws DomainException 退料单不存在时抛出
   */
  public ProductionReturn modify(
      long id,
      long workOrderId,
      String workOrderNumber,
      @Nullable Long operatorId,
      @Nullable String remark) {
    ProductionReturn model = getExist(id);
    model.modify(workOrderId, workOrderNumber, operatorId, remark);
    return model;
  }

  /**
   * 添加退料行（仅草稿可加）
   *
   * @param id 退料单Id
   * @param data 退料行数据
   * @return 生产退料单聚合
   * @throws DomainException 退料单不存在时抛出
   */
  public ProductionReturn addLine(long id, ProductionReturnLineData data) {
    ProductionReturn model = getExist(id);
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
   * 删除退料行（仅草稿可删）
   *
   * @param id 退料单Id
   * @param workOrderMaterialId 工单用料行Id
   * @return 生产退料单聚合
   * @throws DomainException 退料单不存在时抛出
   */
  public ProductionReturn deleteLine(long id, long workOrderMaterialId) {
    ProductionReturn model = getExist(id);
    model.deleteLine(workOrderMaterialId);
    return model;
  }

  /**
   * 退料：核定数量落到行上、计入工单用料的已退数量，单据转入已退料
   *
   * <p>审核与退料合成一步。先让工单侧校验通过再改单据状态——反过来的话，工单侧一抛异常就会留下一张「已退料但没
   * 计入工单」的退料单。退料不能超过已领未耗，这条上限由工单用料行自己说了算，这里不另算一份。
   *
   * <p>货回到行上的仓库由应用层在同一事务里调库存服务完成，本方法只管单据与工单这一半。
   *
   * @param id 退料单Id
   * @param approvals 逐行核定清单，没点到的行按申请数照批
   * @param time 退料时间
   * @return 生产退料单聚合
   * @throws DomainException 退料单或工单不存在、用料行不属于本单、单内没有行、或工单侧数量校验不过时抛出
   */
  public ProductionReturn post(
      long id, List<ProductionReturnApprovalData> approvals, LocalDateTime time) {
    ProductionReturn model = getExist(id);
    WorkOrder workOrder = requireWorkOrder(model.getWorkOrderId());
    model.approve(approvals);
    model.post(time);
    for (ProductionReturnLine line : model.getProductionReturnLines()) {
      workOrder.returnMaterial(line.getWorkOrderMaterialId(), line.getApprovedQty());
    }
    return model;
  }

  /**
   * 红冲：把红字库存单冲掉的量从工单用料的已退数量减回去，单据停在已退料
   *
   * <p>只走已退料的单子。红字单开在库存那边，本方法只补工单这一半。单据不翻状态——它确实退过料，货是库存那边
   * 倒回去的，翻状态等于抹掉这段事实；「被冲过没有」去库存那边查指着它的红字单。
   *
   * <p>可以分次冲，每次只传这次冲掉的几行几量。累计上限由库存那边管，这里只管单次不超本行核定数。
   *
   * @param id 退料单Id
   * @param lines 本次冲回的行与数量（数量为正）
   * @return 生产退料单聚合
   * @throws DomainException 退料单或工单不存在、单据不是已退料状态、行不在本单内或重复、数量非法、或工单侧校验不过时抛出
   */
  public ProductionReturn reverse(long id, List<ProductionReturnReversalData> lines) {
    ProductionReturn model = getExist(id);
    if (!model.isPosted()) {
      throw new DomainException("退料单" + model.getNumber() + "还没退料，删掉重开就行，不用红冲");
    }
    if (lines.isEmpty()) {
      throw new DomainException("红冲得有行：说清冲哪几行、各冲多少");
    }
    WorkOrder workOrder = requireWorkOrder(model.getWorkOrderId());
    Set<Long> seen = new HashSet<>();
    for (ProductionReturnReversalData data : lines) {
      if (!seen.add(data.workOrderMaterialId())) {
        throw new DomainException("工单用料" + data.workOrderMaterialId() + "重复冲回!");
      }
      ProductionReturnLine line = requireLine(model, data.workOrderMaterialId());
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
      workOrder.unreturnMaterial(line.getWorkOrderMaterialId(), qty);
    }
    return model;
  }

  // ==================== 私有 ====================

  /**
   * 按工单用料行Id取本单里的退料行，不存在则抛异常
   *
   * @param model 退料单聚合
   * @param workOrderMaterialId 工单用料行Id
   * @return 退料行
   * @throws DomainException 该用料行不在本单内时抛出
   */
  private ProductionReturnLine requireLine(ProductionReturn model, long workOrderMaterialId) {
    for (ProductionReturnLine line : model.getProductionReturnLines()) {
      if (Objects.equals(line.getWorkOrderMaterialId(), workOrderMaterialId)
          && !line.getIsDeleted()) {
        return line;
      }
    }
    throw new DomainException("工单用料" + workOrderMaterialId + "不在退料单" + model.getNumber() + "中!");
  }

  /**
   * 取退料单，不存在则抛异常
   *
   * @param id 退料单Id
   * @return 生产退料单聚合
   * @throws DomainException 退料单不存在时抛出
   */
  private ProductionReturn getExist(long id) {
    ProductionReturn model = productionReturnRepository.get(id);
    if (model == null) {
      throw new DomainException("退料单" + id + "不存在!");
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
