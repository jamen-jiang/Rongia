package com.rongia.system.application.menu.query;

import com.rongia.shared.application.cqrs.Query;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;

/** 按Id查询菜单 */
@NullMarked
@Getter
@Setter
@Schema(description = "按Id查询菜单")
public class GetMenuByIdQuery implements Query {

  /** 菜单Id */
  @Schema(description = "菜单Id", requiredMode = Schema.RequiredMode.REQUIRED)
  private Long id;

  /** 无参构造：供 Jackson 反序列化用 */
  public GetMenuByIdQuery() {}

  /**
   * 有参构造
   *
   * @param id 菜单Id
   */
  public GetMenuByIdQuery(long id) {
    this.id = id;
  }
}
