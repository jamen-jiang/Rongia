package com.rongia.basedata.domain.unit;

import com.rongia.shared.domain.exception.DomainException;
import java.math.BigDecimal;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 单位工厂，负责创建单位 */
@NullMarked
public class UnitFactory {

  /** 单位仓储 */
  private final UnitRepository unitRepository;

  /**
   * 构造器注入
   *
   * @param unitRepository 单位仓储
   */
  public UnitFactory(UnitRepository unitRepository) {
    this.unitRepository = unitRepository;
  }

  /**
   * 创建单位
   *
   * @param parentId 参考单位Id（可空，空表示根）
   * @param parentPath 层级路径（可空）
   * @param code 代码
   * @param name 名称
   * @param relativeFactor 包含几个参考单位
   * @param parentFactor 参考单位的绝对系数（可空，根单位传 null）
   * @param rounding 数量落数步长
   * @param disabled 是否禁用
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   * @return 单位聚合
   * @throws DomainException 单位代码已存在时抛出
   */
  public Unit create(
      @Nullable Long parentId,
      @Nullable String parentPath,
      String code,
      String name,
      BigDecimal relativeFactor,
      @Nullable BigDecimal parentFactor,
      BigDecimal rounding,
      boolean disabled,
      @Nullable Integer sequence,
      @Nullable String remark) {
    if (unitRepository.isExistByCodeExcludeId(code)) {
      throw new DomainException("单位代码" + code + "已存在!");
    }
    return new Unit(
        parentId,
        parentPath,
        code,
        name,
        relativeFactor,
        parentFactor,
        rounding,
        disabled,
        sequence,
        remark);
  }
}
