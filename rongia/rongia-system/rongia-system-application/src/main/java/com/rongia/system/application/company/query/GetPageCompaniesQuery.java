package com.rongia.system.application.company.query;

import com.rongia.shared.application.cqrs.Query;
import com.rongia.shared.application.request.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 分页查询公司列表 */
@NullMarked
@Getter
@Setter
@Schema(description = "分页查询公司列表")
public class GetPageCompaniesQuery extends PageRequest implements Query {

  /** 公司名称（可空，模糊匹配） */
  @Schema(description = "公司名称")
  @Nullable
  private String name;
}
