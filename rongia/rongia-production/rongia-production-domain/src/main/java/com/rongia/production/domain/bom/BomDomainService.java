package com.rongia.production.domain.bom;

import com.rongia.production.domain.routing.RoutingRepository;
import com.rongia.shared.domain.exception.DomainException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** BOM 领域服务，负责跨聚合编排与用料行同步 */
@NullMarked
public class BomDomainService {

  /** BOM 工厂 */
  private final BomFactory bomFactory;

  /** BOM 仓储 */
  private final BomRepository bomRepository;

  /** 工艺路线仓储 */
  private final RoutingRepository routingRepository;

  /**
   * 构造器注入
   *
   * @param bomFactory BOM 工厂
   * @param bomRepository BOM 仓储
   * @param routingRepository 工艺路线仓储
   */
  public BomDomainService(
      BomFactory bomFactory,
      BomRepository bomRepository,
      RoutingRepository routingRepository) {
    this.bomFactory = bomFactory;
    this.bomRepository = bomRepository;
    this.routingRepository = routingRepository;
  }

  /**
   * 导入生成：父件SKU + 版本已存在则修改，不存在则创建
   *
   * @param type BOM 类型编码
   * @param code 参考号（可空）
   * @param version 版本号
   * @param materialId 父件SPU
   * @param variantId 父件SKU（可空）
   * @param routingId 工艺路线Id（可空）
   * @param qty 产出数量
   * @param unitId 产出单位Id
   * @param startMode 开工方式编码
   * @param issueMode 领料方式编码
   * @param leadDays 制造天数（天）
   * @param prepareDays 备料天数（天）
   * @param batchSize 自动生成工单的批量（可空）
   * @param disabled 是否禁用
   * @param remark 备注（可空）
   * @return BOM 聚合
   */
  public Bom importGenerate(
      String type,
      @Nullable String code,
      String version,
      long materialId,
      @Nullable Long variantId,
      @Nullable Long routingId,
      BigDecimal qty,
      long unitId,
      String startMode,
      String issueMode,
      int leadDays,
      int prepareDays,
      @Nullable BigDecimal batchSize,
      boolean disabled,
      @Nullable String remark) {
    requireRouting(routingId);

    // 按父件SKU + 版本查 BOM
    Bom model = bomRepository.getByMaterialIdAndVariantIdAndVersion(materialId, variantId, version);

    // 不存在则创建，存在则修改
    if (model == null) {
      model =
          bomFactory.create(
              type,
              code,
              version,
              materialId,
              variantId,
              routingId,
              qty,
              unitId,
              startMode,
              issueMode,
              leadDays,
              prepareDays,
              batchSize,
              disabled,
              remark);
    } else {
      modify(
          model,
          type,
          code,
          version,
          materialId,
          variantId,
          routingId,
          qty,
          unitId,
          startMode,
          issueMode,
          leadDays,
          prepareDays,
          batchSize,
          disabled,
          remark);
    }

    return model;
  }

  /**
   * 修改 BOM 表头
   *
   * @param id BOM Id
   * @param type BOM 类型编码
   * @param code 参考号（可空）
   * @param version 版本号
   * @param materialId 父件SPU
   * @param variantId 父件SKU（可空）
   * @param routingId 工艺路线Id（可空）
   * @param qty 产出数量
   * @param unitId 产出单位Id
   * @param startMode 开工方式编码
   * @param issueMode 领料方式编码
   * @param leadDays 制造天数（天）
   * @param prepareDays 备料天数（天）
   * @param batchSize 自动生成工单的批量（可空）
   * @param disabled 是否禁用
   * @param remark 备注（可空）
   * @return BOM 聚合
   * @throws DomainException BOM 不存在时抛出
   */
  public Bom modify(
      long id,
      String type,
      @Nullable String code,
      String version,
      long materialId,
      @Nullable Long variantId,
      @Nullable Long routingId,
      BigDecimal qty,
      long unitId,
      String startMode,
      String issueMode,
      int leadDays,
      int prepareDays,
      @Nullable BigDecimal batchSize,
      boolean disabled,
      @Nullable String remark) {
    Bom model = getExist(id);
    modify(
        model,
        type,
        code,
        version,
        materialId,
        variantId,
        routingId,
        qty,
        unitId,
        startMode,
        issueMode,
        leadDays,
        prepareDays,
        batchSize,
        disabled,
        remark);
    return model;
  }

  /**
   * 修改 BOM 表头
   *
   * @param model BOM 聚合
   * @param type BOM 类型编码
   * @param code 参考号（可空）
   * @param version 版本号
   * @param materialId 父件SPU
   * @param variantId 父件SKU（可空）
   * @param routingId 工艺路线Id（可空）
   * @param qty 产出数量
   * @param unitId 产出单位Id
   * @param startMode 开工方式编码
   * @param issueMode 领料方式编码
   * @param leadDays 制造天数（天）
   * @param prepareDays 备料天数（天）
   * @param batchSize 自动生成工单的批量（可空）
   * @param disabled 是否禁用
   * @param remark 备注（可空）
   * @throws DomainException 父件SKU + 版本与其它 BOM 冲突时抛出
   */
  public void modify(
      Bom model,
      String type,
      @Nullable String code,
      String version,
      long materialId,
      @Nullable Long variantId,
      @Nullable Long routingId,
      BigDecimal qty,
      long unitId,
      String startMode,
      String issueMode,
      int leadDays,
      int prepareDays,
      @Nullable BigDecimal batchSize,
      boolean disabled,
      @Nullable String remark) {
    requireRouting(routingId);
    // 校验父件SKU + 版本唯一
    if (bomRepository.isExistByMaterialIdAndVariantIdAndVersionExcludeId(
        materialId, variantId, version, model.getId())) {
      throw new DomainException("该父件的 BOM 版本" + version + "已存在!");
    }
    model.modify(
        type,
        code,
        version,
        materialId,
        variantId,
        routingId,
        qty,
        unitId,
        startMode,
        issueMode,
        leadDays,
        prepareDays,
        batchSize,
        disabled,
        remark);
  }

  /**
   * 复制：把一份 BOM 的表头与用料行整体复制成新的一版
   *
   * <p>改配方不动老版本：老版本可能还挂着在制工单，复制出来换成新版本号再改，历史就还能按版本回溯。
   *
   * @param id 源 BOM Id
   * @param version 新版本号
   * @param remark 备注（可空）
   * @return 新 BOM（由调用方落库）
   * @throws DomainException 源 BOM 不存在、或新版本号已存在时抛出
   */
  public Bom copy(
      long id,
      String version,
      @Nullable String remark) {
    Bom source = getExist(id);
    Bom model =
        bomFactory.create(
            source.getType(),
            source.getCode(),
            version,
            source.getMaterialId(),
            source.getVariantId(),
            source.getRoutingId(),
            source.getQty(),
            source.getUnitId(),
            source.getStartMode(),
            source.getIssueMode(),
            source.getLeadDays(),
            source.getPrepareDays(),
            source.getBatchSize(),
            Boolean.TRUE.equals(source.getDisabled()),
            remark);
    for (BomLine line : source.getBomLines()) {
      model.addLine(
          line.getMaterialId(),
          line.getVariantId(),
          line.getRoutingOperationId(),
          line.getQty(),
          line.getLossRate(),
          line.getUnitId(),
          line.getSequence());
    }
    return model;
  }

  // ==================== 私有 ====================

  /**
   * 校验引用的工艺路线存在（可空：为空表示下达时由工单指定）
   *
   * @param routingId 工艺路线Id（可空）
   * @throws DomainException 工艺路线不存在时抛出
   */
  private void requireRouting(@Nullable Long routingId) {
    if (routingId == null) {
      return;
    }
    if (routingRepository.get(routingId) == null) {
      throw new DomainException("工艺路线" + routingId + "不存在!");
    }
  }

  /**
   * 取 BOM，不存在则抛异常
   *
   * @param id BOM Id
   * @return BOM 聚合
   * @throws DomainException 不存在时抛出
   */
  private Bom getExist(long id) {
    Bom model = bomRepository.get(id);
    if (model == null) {
      throw new DomainException("BOM" + id + "不存在!");
    }
    return model;
  }

  /**
   * 同步用料行（按行号匹配：删除不在输入中的、更新已存在的、添加新的）
   *
   * @param bom BOM 聚合
   * @param lines 目标用料行列表（可空，null 视为清空）
   */
  public void syncLines(Bom bom, @Nullable List<BomLineData> lines) {
    List<BomLineData> target = lines == null ? List.of() : lines;

    // 1. 删除未在输入中出现的行（按行号匹配）
    List<BomLine> toRemoves =
        bom.getBomLines().stream()
            .filter(
                i ->
                    target.stream()
                        .noneMatch(d -> Objects.equals(d.sequence(), i.getSequence())))
            .toList();
    for (BomLine line : toRemoves) {
      bom.deleteLine(line.getId());
    }

    // 2. 建立"行号 → 已存在行Id"的映射（只统计未删除的）
    Map<Integer, Long> existingDict =
        bom.getBomLines().stream()
            .filter(i -> !i.getIsDeleted())
            .collect(Collectors.toMap(BomLine::getSequence, BomLine::getId, (a, b) -> a));

    // 3. 遍历输入：存在则修改，不存在则添加
    for (BomLineData data : target) {
      Long id = existingDict.get(data.sequence());
      if (id != null) {
        // 存在：修改
        bom.modifyLine(
            id,
            data.materialId(),
            data.variantId(),
            data.routingOperationId(),
            data.qty(),
            data.lossRate(),
            data.unitId(),
            data.sequence());
      } else {
        // 不存在：添加
        bom.addLine(
            data.materialId(),
            data.variantId(),
            data.routingOperationId(),
            data.qty(),
            data.lossRate(),
            data.unitId(),
            data.sequence());
      }
    }
  }
}
