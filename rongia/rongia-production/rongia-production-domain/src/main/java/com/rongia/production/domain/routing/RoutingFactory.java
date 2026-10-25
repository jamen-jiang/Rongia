package com.rongia.production.domain.routing;

import com.rongia.shared.domain.exception.DomainException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 工艺路线工厂，负责创建工艺路线 */
@NullMarked
public class RoutingFactory {

  /** 工艺路线仓储 */
  private final RoutingRepository routingRepository;

  /**
   * 构造器注入
   *
   * @param routingRepository 工艺路线仓储
   */
  public RoutingFactory(RoutingRepository routingRepository) {
    this.routingRepository = routingRepository;
  }

  /**
   * 创建工艺路线
   *
   * @param code 路线代码
   * @param name 路线名称
   * @param version 版本号
   * @param disabled 是否禁用
   * @param remark 备注（可空）
   * @return 工艺路线聚合
   * @throws DomainException 代码 + 版本已存在时抛出
   */
  public Routing create(
      String code,
      String name,
      String version,
      boolean disabled,
      @Nullable String remark) {
    // 校验代码 + 版本唯一
    if (routingRepository.isExistByCodeAndVersionExcludeId(code, version)) {
      throw new DomainException("工艺路线" + code + "的版本" + version + "已存在!");
    }
    return new Routing(code, name, version, disabled, remark);
  }
}
