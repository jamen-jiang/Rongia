package com.rongia.production.domain.workorder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rongia.production.domain.bom.Bom;
import com.rongia.production.domain.bom.BomFactory;
import com.rongia.production.domain.bom.BomLineData;
import com.rongia.production.domain.bom.BomRepository;
import com.rongia.production.domain.bom.BomTypeEnum;
import com.rongia.production.domain.operation.OperationRepository;
import com.rongia.production.domain.routing.RoutingRepository;
import com.rongia.production.domain.workcenter.WorkCenterRepository;
import com.rongia.shared.domain.exception.DomainException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** BOM 递归展开（套件穿透、去重合并、防环）的纯算法测试 */
@DisplayName("工单 BOM 展开")
class WorkOrderDomainServiceTest {

  private static final long UNIT = 1L;

  private final BomRepository bomRepository = mock(BomRepository.class);

  private final WorkOrderDomainService service =
      new WorkOrderDomainService(
          mock(WorkOrderFactory.class),
          mock(WorkOrderRepository.class),
          bomRepository,
          mock(RoutingRepository.class),
          mock(OperationRepository.class),
          mock(WorkCenterRepository.class));

  /** 造一张 BOM，用给定的类型与产出数量 */
  private Bom bom(String type, String qty, BomLineData... lines) {
    Bom model =
        new BomFactory(bomRepository)
            .create(
                type,
                null,
                "V1",
                /* materialId */ 1L,
                /* variantId */ 1L,
                /* routingId */ null,
                new BigDecimal(qty),
                UNIT,
                "AllAvailable",
                "Strict",
                0,
                0,
                null,
                false,
                null);
    for (BomLineData line : lines) {
      model.addLine(
          line.materialId(),
          line.variantId(),
          line.routingOperationId(),
          line.qty(),
          line.lossRate(),
          line.unitId(),
          line.sequence());
    }
    return model;
  }

  /** 一条用料：子件 SPU = SKU = variantId */
  private static BomLineData line(long variantId, String qty, int sequence) {
    return new BomLineData(
        variantId,
        variantId,
        null,
        new BigDecimal(qty),
        BigDecimal.ZERO,
        UNIT,
        sequence);
  }

  @Test
  @DisplayName("普通子件不穿透，只展开一层")
  void normalComponentStaysOneLevel() {
    Bom root = bom(BomTypeEnum.NORMAL.getCode(), "1", line(2L, "3", 1));

    List<WorkOrderDomainService.MaterialRequirement> result =
        service.resolveMaterials(new BigDecimal("10"), root);

    assertEquals(1, result.size());
    assertEquals(2L, result.getFirst().variantId());
    assertEquals(0, new BigDecimal("30").compareTo(result.getFirst().requiredQty()));
  }

  @Test
  @DisplayName("套件穿透：套件本身不成行，子件按折算数量上来")
  void phantomComponentIsExpanded() {
    // 套件 2：产出 1 个，用 4 个件 3
    Bom phantom = bom(BomTypeEnum.PHANTOM.getCode(), "1", line(3L, "4", 1));
    when(bomRepository.getEnabledByVariantId(anyLong(), eq(2L))).thenReturn(phantom);

    // 根：产出 1 个，用 2 个套件 2
    Bom root = bom(BomTypeEnum.NORMAL.getCode(), "1", line(2L, "2", 1));

    List<WorkOrderDomainService.MaterialRequirement> result =
        service.resolveMaterials(new BigDecimal("5"), root);

    assertEquals(1, result.size(), "套件自己不该成行");
    assertEquals(3L, result.getFirst().variantId());
    assertEquals(0, new BigDecimal("40").compareTo(result.getFirst().requiredQty()));
  }

  @Test
  @DisplayName("同一子件经两条套件路径出现时合并数量")
  void duplicatedComponentIsMerged() {
    Bom phantomA = bom(BomTypeEnum.PHANTOM.getCode(), "1", line(9L, "1", 1));
    Bom phantomB = bom(BomTypeEnum.PHANTOM.getCode(), "1", line(9L, "3", 1));
    when(bomRepository.getEnabledByVariantId(anyLong(), eq(2L))).thenReturn(phantomA);
    when(bomRepository.getEnabledByVariantId(anyLong(), eq(4L))).thenReturn(phantomB);

    Bom root =
        bom(BomTypeEnum.NORMAL.getCode(), "1", line(2L, "1", 1), line(4L, "1", 2));

    List<WorkOrderDomainService.MaterialRequirement> result =
        service.resolveMaterials(new BigDecimal("1"), root);

    assertEquals(1, result.size(), "同一 SKU 应合成一行");
    assertEquals(9L, result.getFirst().variantId());
    assertEquals(0, new BigDecimal("4").compareTo(result.getFirst().requiredQty()));
  }

  @Test
  @DisplayName("套件循环引用直接拒绝")
  void cyclicPhantomIsRejected() {
    Bom phantomA = bom(BomTypeEnum.PHANTOM.getCode(), "1", line(2L, "1", 1));
    Bom root = bom(BomTypeEnum.NORMAL.getCode(), "1", line(2L, "1", 1));
    when(bomRepository.getEnabledByVariantId(anyLong(), eq(2L))).thenReturn(phantomA);
    when(bomRepository.getEnabledByVariantId(anyLong(), anyLong())).thenReturn(phantomA);

    assertThrows(
        DomainException.class, () -> service.resolveMaterials(new BigDecimal("1"), root));
  }

  @Test
  @DisplayName("同一子件展开后单位不一致时报错，不做换算")
  void unitMismatchIsRejected() {
    Bom phantomA = bom(BomTypeEnum.PHANTOM.getCode(), "1", line(9L, "1", 1));
    when(bomRepository.getEnabledByVariantId(anyLong(), eq(2L))).thenReturn(phantomA);

    Bom root =
        new BomFactory(bomRepository)
            .create(
                BomTypeEnum.NORMAL.getCode(),
                null,
                "V1",
                1L,
                1L,
                null,
                BigDecimal.ONE,
                UNIT,
                "AllAvailable",
                "Strict",
                0,
                0,
                null,
                false,
                null);
    root.addLine(9L, 9L, null, BigDecimal.ONE, BigDecimal.ZERO, 2L, 1);
    root.addLine(2L, 2L, null, BigDecimal.ONE, BigDecimal.ZERO, UNIT, 2);

    assertThrows(
        DomainException.class, () -> service.resolveMaterials(BigDecimal.ONE, root));
  }

  @Test
  @DisplayName("物料没有 BOM 时不消耗任何用料")
  void noBomMeansNoMaterial() {
    assertEquals(
        0, service.resolveMaterials(BigDecimal.TEN, null).size());
  }

  @Test
  @DisplayName("结果按 SKU 去重后保持首次出现的顺序")
  void orderFollowsFirstAppearance() {
    Bom phantom = bom(BomTypeEnum.PHANTOM.getCode(), "1", line(3L, "1", 1));
    when(bomRepository.getEnabledByVariantId(anyLong(), eq(2L))).thenReturn(phantom);

    Bom root = bom(BomTypeEnum.NORMAL.getCode(), "1", line(2L, "1", 1), line(5L, "1", 2));

    List<Long> variantIds =
        service.resolveMaterials(BigDecimal.ONE, root).stream()
            .map(WorkOrderDomainService.MaterialRequirement::variantId)
            .toList();

    assertEquals(List.of(3L, 5L), variantIds);
  }
}
