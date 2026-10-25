package com.rongia.system.application.user.query;

import com.rongia.shared.application.cqrs.Query;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;

/** 按Id查询用户 */
@NullMarked
@Getter
@Setter
@Schema(description = "按Id查询用户")
public class GetUserByIdQuery implements Query {

  /** 用户Id */
  @Schema(description = "用户Id", requiredMode = Schema.RequiredMode.REQUIRED)
  private Long id;

  /** 无参构造 */
  public GetUserByIdQuery() {}

  /**
   * 有参构造
   *
   * @param id 用户Id
   */
  public GetUserByIdQuery(long id) {
    this.id = id;
  }
}
