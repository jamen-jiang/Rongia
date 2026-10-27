package com.rongia.stock.domain.stockentrytype;

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
   * @param code 类型编码
   * @param name 类型名称
   * @param direction 方向编码，取值见 {@link StockEntryDirectionEnum}
   * @param disabled 是否停用
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   * @return 库存单据类型聚合
   * @throws DomainException 类型名称或编码已存在时抛出
   */
  public StockEntryType create(
      String code,
      String name,
      String direction,
      boolean disabled,
      @Nullable Integer sequence,
      @Nullable String remark) {
    if (stockEntryTypeRepository.isExistByNameExcludeId(name)) {
      throw new DomainException("库存单据类型" + name + "已存在!");
    }
    if (stockEntryTypeRepository.getByCode(code) != null) {
      throw new DomainException("类型编码" + code + "已存在!");
    }
    return new StockEntryType(code, name, direction, disabled, sequence, remark);
  }
}
