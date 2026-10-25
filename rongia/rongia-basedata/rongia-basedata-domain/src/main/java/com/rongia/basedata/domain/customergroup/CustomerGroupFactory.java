package com.rongia.basedata.domain.customergroup;

import com.rongia.shared.domain.exception.DomainException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 客户组工厂，负责创建客户组 */
@NullMarked
public class CustomerGroupFactory {

  /** 客户组仓储 */
  private final CustomerGroupRepository customerGroupRepository;

  /**
   * 构造器注入
   *
   * @param customerGroupRepository 客户组仓储
   */
  public CustomerGroupFactory(CustomerGroupRepository customerGroupRepository) {
    this.customerGroupRepository = customerGroupRepository;
  }

  /**
   * 创建客户组
   *
   * @param code 代码
   * @param name 名称
   * @param disabled 是否禁用
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   * @return 客户组聚合
   * @throws DomainException 代码已存在时抛出
   */
  public CustomerGroup create(
      String code,
      String name,
      boolean disabled,
      @Nullable Integer sequence,
      @Nullable String remark) {
    if (customerGroupRepository.isExistByCodeExcludeId(code)) {
      throw new DomainException("客户组代码" + code + "已存在!");
    }
    return new CustomerGroup(code, name, disabled, sequence, remark);
  }
}
