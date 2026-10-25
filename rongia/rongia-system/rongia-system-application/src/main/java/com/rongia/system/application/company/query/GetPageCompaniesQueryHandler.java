package com.rongia.system.application.company.query;

import com.rongia.shared.application.response.PageResponse;
import org.axonframework.messaging.queryhandling.annotation.QueryHandler;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;

/** 分页查询公司列表查询处理器 */
@NullMarked
@Component
public class GetPageCompaniesQueryHandler {

  /** 公司查询服务 */
  private final CompanyQueryService companyQueryService;

  /**
   * 构造器注入
   *
   * @param companyQueryService 公司查询服务
   */
  public GetPageCompaniesQueryHandler(CompanyQueryService companyQueryService) {
    this.companyQueryService = companyQueryService;
  }

  /**
   * 处理查询
   *
   * @param query 查询
   * @return 分页响应
   */
  @QueryHandler
  public PageResponse<CompanyResponse> handle(GetPageCompaniesQuery query) {
    return companyQueryService.getPage(query);
  }
}
