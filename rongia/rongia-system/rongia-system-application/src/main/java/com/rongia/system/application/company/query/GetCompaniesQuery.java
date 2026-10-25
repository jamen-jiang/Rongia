package com.rongia.system.application.company.query;

import com.rongia.shared.application.cqrs.Query;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 获取公司列表 */
@NullMarked
@Getter
@Setter
@Schema(description = "获取公司列表")
public class GetCompaniesQuery implements Query {

  /** 区域（可空） */
  @Schema(description = "区域")
  @Nullable
  private String region;

  /** 组别（可空） */
  @Schema(description = "组别")
  @Nullable
  private String group;

  /** 无参构造 */
  public GetCompaniesQuery() {}

  /**
   * 有参构造
   *
   * @param region 区域（可空）
   * @param group 组别（可空）
   */
  public GetCompaniesQuery(@Nullable String region, @Nullable String group) {
    this.region = region;
    this.group = group;
  }
}
