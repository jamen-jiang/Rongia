package com.rongia.inventory.domain.stockentrytype;

import com.rongia.shared.domain.exception.DomainException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 库存单据类型工厂，负责创建类型 */
@NullMarked
public class StockEntryTypeFactory {

  /** 库存单据类型仓储 */
  private final StockEntryTypeRepository stockEntryTypeRepository;

  /**
   * 构造器注入
   *
   * @param stockEntryTypeRepository 库存单据类型仓储
   */
  public StockEntryTypeFactory(StockEntryTypeRepository stockEntryTypeRepository) {
    this.stockEntryTypeRepository = stockEntryTypeRepository;
  }

  /**
   * 创建库存单据类型
   *
   * @param code 类型编码，取值见 {@link StockEntryTypeEnum}
   * @param name 类型名称
   * @param isStandard 是否标准类型
   * @param disabled 是否停用
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   * @return 库存单据类型聚合
   * @throws DomainException 类型名称已存在时抛出
   */
  public StockEntryType create(
      String code,
      String name,
      boolean isStandard,
      boolean disabled,
      @Nullable Integer sequence,
      @Nullable String remark) {
    if (stockEntryTypeRepository.isExistByNameExcludeId(name)) {
      throw new DomainException("库存单据类型" + name + "已存在!");
    }
    return new StockEntryType(code, name, isStandard, disabled, sequence, remark);
  }
}
