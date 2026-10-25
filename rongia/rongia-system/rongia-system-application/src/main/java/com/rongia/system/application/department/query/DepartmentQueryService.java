package com.rongia.system.application.department.query;

import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 部门查询服务接口 */
@NullMarked
public interface DepartmentQueryService {

  /**
   * 按Id查询
   *
   * @param id 部门Id
   * @return 部门响应
   */
  @Nullable DepartmentResponse getById(long id);

  /**
   * 查询部门列表（含公司 / 父部门）
   *
   * @param query 查询条件（可空）
   * @return 部门响应列表
   */
  List<DepartmentResponse> getList(GetDepartmentsQuery query);
}
