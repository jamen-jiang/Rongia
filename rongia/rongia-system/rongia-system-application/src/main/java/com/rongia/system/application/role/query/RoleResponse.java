package com.rongia.system.application.role.query;

import com.rongia.shared.application.response.BaseResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 角色响应 */
@NullMarked
@Getter
@Setter
@Schema(description = "角色响应")
public class RoleResponse extends BaseResponse {

  /** 角色Id */
  @Schema(description = "角色Id", requiredMode = Schema.RequiredMode.REQUIRED)
  private Long id;

  /** 角色名称 */
  @Schema(description = "角色名称", requiredMode = Schema.RequiredMode.REQUIRED)
  private String name = "";

  /** 备注（可空） */
  @Schema(description = "备注")
  @Nullable
  private String remark;
}
