package com.rongia.system.application.company.query;

import org.axonframework.messaging.queryhandling.annotation.QueryHandler;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/** 按Id获取公司查询处理器 */
@NullMarked
@Component
public class GetCompanyByIdQueryHandler {

  /** 公司查询服务 */
  private final CompanyQueryService companyQueryService;

  /**
   * 构造器注入
   *
   * @param companyQueryService 公司查询服务
   */
  public GetCompanyByIdQueryHandler(CompanyQueryService companyQueryService) {
    this.companyQueryService = companyQueryService;
  }

  /**
   * 处理查询
   *
   * @param query 查询
   * @return 公司响应
   */
  @QueryHandler
  public @Nullable CompanyResponse handle(GetCompanyByIdQuery query) {
    return companyQueryService.getById(query.getId());
  }
}
