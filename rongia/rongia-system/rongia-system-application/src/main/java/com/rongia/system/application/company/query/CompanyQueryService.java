package com.rongia.system.application.company.query;

import com.rongia.shared.application.response.PageResponse;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 公司查询服务接口 */
@NullMarked
public interface CompanyQueryService {

  /**
   * 按Id查询公司（含联系人）
   *
   * @param id 公司Id
   * @return 公司响应，不存在返回 null
   */
  @Nullable CompanyResponse getById(long id);

  /**
   * 查询公司列表（按排序字段升序，无排序的排最后）
   *
   * @param query 查询条件
   * @return 公司响应列表
   */
  List<CompanyResponse> getList(GetCompaniesQuery query);

  /**
   * 分页查询公司
   *
   * @param query 分页查询条件
   * @return 分页响应
   */
  PageResponse<CompanyResponse> getPage(GetPageCompaniesQuery query);
}
