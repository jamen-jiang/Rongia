package com.rongia.system.application.company.query;

import com.rongia.shared.application.cqrs.Query;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;

/** 按Id获取公司 */
@NullMarked
@Getter
@Setter
@Schema(description = "按Id获取公司")
public class GetCompanyByIdQuery implements Query {

  /** 公司Id */
  @Schema(description = "公司Id", requiredMode = Schema.RequiredMode.REQUIRED)
  private Long id;

  /** 无参构造 */
  public GetCompanyByIdQuery() {}

  /**
   * 有参构造
   *
   * @param id 公司Id
   */
  public GetCompanyByIdQuery(long id) {
    this.id = id;
  }
}
