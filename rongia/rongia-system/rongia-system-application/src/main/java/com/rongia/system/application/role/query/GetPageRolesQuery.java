package com.rongia.system.application.role.query;

import com.rongia.shared.application.request.PageRequestWithUnique;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 分页获取角色列表 */
@NullMarked
@Getter
@Setter
@Schema(description = "分页获取角色列表")
public class GetPageRolesQuery extends PageRequestWithUnique<Long> {

  /** 角色名称（可空） */
  @Schema(description = "角色名称")
  @Nullable
  private String name;
}
