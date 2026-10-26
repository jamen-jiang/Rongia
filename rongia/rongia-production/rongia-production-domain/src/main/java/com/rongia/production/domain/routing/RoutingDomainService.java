package com.rongia.production.domain.routing;

import com.rongia.shared.domain.exception.DomainException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 工艺路线领域服务，负责跨聚合编排 */
@NullMarked
public class RoutingDomainService {

  /** 工艺路线工厂 */
  private final RoutingFactory routingFactory;

  /** 工艺路线仓储 */
  private final RoutingRepository routingRepository;

  /**
   * 构造器注入
   *
   * @param routingFactory 工艺路线工厂
   * @param routingRepository 工艺路线仓储
   */
  public RoutingDomainService(
      RoutingFactory routingFactory, RoutingRepository routingRepository) {
    this.routingFactory = routingFactory;
    this.routingRepository = routingRepository;
  }

  /**
   * 导入生成：名称已存在则修改，不存在则创建
   *
   * @param name 路线名称
   * @param disabled 是否禁用
   * @param remark 备注（可空）
   * @return 工艺路线聚合
   */
  public Routing importGenerate(String name, boolean disabled, @Nullable String remark) {
    // 按名称查工艺路线
    Routing model = routingRepository.getByName(name);

    // 不存在则创建，存在则修改
    if (model == null) {
      model = routingFactory.create(name, disabled, remark);
    } else {
      modify(model, name, disabled, remark);
    }

    return model;
  }

  /**
   * 修改工艺路线
   *
   * @param id 工艺路线Id
   * @param name 路线名称
   * @param disabled 是否禁用
   * @param remark 备注（可空）
   * @return 工艺路线聚合
   * @throws DomainException 工艺路线不存在时抛出
   */
  public Routing modify(long id, String name, boolean disabled, @Nullable String remark) {
    Routing model = getExist(id);
    modify(model, name, disabled, remark);
    return model;
  }

  /**
   * 修改工艺路线
   *
   * @param model 工艺路线
   * @param name 路线名称
   * @param disabled 是否禁用
   * @param remark 备注（可空）
   * @throws DomainException 名称已被其它路线占用时抛出
   */
  public void modify(Routing model, String name, boolean disabled, @Nullable String remark) {
    // 校验名称唯一
    if (routingRepository.isExistByNameExcludeId(name, model.getId())) {
      throw new DomainException("工艺路线" + name + "已存在!");
    }
    model.modify(name, disabled, remark);
  }

  // ==================== 私有 ====================

  /**
   * 取工艺路线，不存在则抛异常
   *
   * @param id 工艺路线Id
   * @return 工艺路线聚合
   * @throws DomainException 不存在时抛出
   */
  private Routing getExist(long id) {
    Routing model = routingRepository.get(id);
    if (model == null) {
      throw new DomainException("工艺路线" + id + "不存在!");
    }
    return model;
  }
}
