package com.rongia.basedata.domain.customergroup;

import com.rongia.shared.domain.exception.DomainException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 客户组领域服务 */
@NullMarked
public class CustomerGroupDomainService {

  /** 客户组工厂 */
  private final CustomerGroupFactory customerGroupFactory;

  /** 客户组仓储 */
  private final CustomerGroupRepository customerGroupRepository;

  /**
   * 构造器注入
   *
   * @param customerGroupFactory 客户组工厂
   * @param customerGroupRepository 客户组仓储
   */
  public CustomerGroupDomainService(
      CustomerGroupFactory customerGroupFactory, CustomerGroupRepository customerGroupRepository) {
    this.customerGroupFactory = customerGroupFactory;
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
   */
  public CustomerGroup create(
      String code,
      String name,
      boolean disabled,
      @Nullable Integer sequence,
      @Nullable String remark) {
    return customerGroupFactory.create(code, name, disabled, sequence, remark);
  }

  /**
   * 修改客户组
   *
   * @param id 客户组Id
   * @param code 代码
   * @param name 名称
   * @param disabled 是否禁用
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   * @return 客户组聚合
   * @throws DomainException 客户组不存在时抛出
   */
  public CustomerGroup modify(
      long id,
      String code,
      String name,
      boolean disabled,
      @Nullable Integer sequence,
      @Nullable String remark) {
    CustomerGroup model = getExist(id);
    if (customerGroupRepository.isExistByCodeExcludeId(code, id)) {
      throw new DomainException("客户组代码" + code + "已存在!");
    }
    model.modify(code, name, disabled, sequence, remark);
    return model;
  }

  /**
   * 删除客户组（逻辑删除）
   *
   * @param id 客户组Id
   * @return 客户组聚合（已标记删除）
   * @throws DomainException 客户组不存在时抛出
   */
  public CustomerGroup delete(long id) {
    CustomerGroup model = getExist(id);
    model.delete();
    return model;
  }

  /**
   * 按Id取客户组
   *
   * @param id 客户组Id
   * @return 客户组聚合
   * @throws DomainException 客户组不存在时抛出
   */
  private CustomerGroup getExist(long id) {
    CustomerGroup model = customerGroupRepository.get(id);
    if (model == null) {
      throw new DomainException("客户组" + id + "不存在!");
    }
    return model;
  }
}
