package com.rongia.system.application.company.query;

import com.rongia.shared.application.response.PageResponse;
import java.util.List;
import org.axonframework.messaging.queryhandling.annotation.QueryHandler;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;

/** 获取公司列表查询处理器 */
@NullMarked
@Component
public class GetCompaniesQueryHandler {

  /** 公司查询服务 */
  private final CompanyQueryService companyQueryService;

  /**
   * 构造器注入
   *
   * @param companyQueryService 公司查询服务
   */
  public GetCompaniesQueryHandler(CompanyQueryService companyQueryService) {
    this.companyQueryService = companyQueryService;
  }

  /**
   * 处理查询
   *
   * @param query 查询
   * @return 公司响应列表
   */
  @QueryHandler
  public List<CompanyResponse> handle(GetCompaniesQuery query) {
    return companyQueryService.getList(query);
  }
}
