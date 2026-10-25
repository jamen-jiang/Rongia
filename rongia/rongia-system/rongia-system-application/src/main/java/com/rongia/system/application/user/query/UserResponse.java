package com.rongia.system.application.user.query;

import com.rongia.shared.application.response.BaseResponse;
import com.rongia.system.application.role.query.RoleResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 用户响应 */
@NullMarked
@Getter
@Setter
@Schema(description = "用户响应")
public class UserResponse extends BaseResponse {

  /** 用户Id */
  @Schema(description = "用户Id", requiredMode = Schema.RequiredMode.REQUIRED)
  @Nullable
  private Long id;

  /** 用户名称 */
  @Schema(description = "用户名称", requiredMode = Schema.RequiredMode.REQUIRED)
  private String name = "";

  /** 账号 */
  @Schema(description = "账号", requiredMode = Schema.RequiredMode.REQUIRED)
  private String userName = "";

  /** 邮箱（可空） */
  @Schema(description = "邮箱")
  @Nullable
  private String email;

  /** 手机号（可空） */
  @Schema(description = "手机号")
  @Nullable
  private String mobile;

  /** 是否禁用 */
  @Schema(description = "是否禁用", requiredMode = Schema.RequiredMode.REQUIRED)
  private boolean disabled;

  /** 密码 */
  @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED)
  private String password = "";

  /** 备注（可空） */
  @Schema(description = "备注")
  @Nullable
  private String remark;

  /** 角色列表 */
  @Schema(description = "角色列表", requiredMode = Schema.RequiredMode.REQUIRED)
  private List<RoleResponse> roles = new ArrayList<>();

  /**
   * 获取角色Id列表
   *
   * @return 角色Id列表
   */
  @Schema(description = "角色Id列表", requiredMode = Schema.RequiredMode.REQUIRED)
  public List<Long> getRoleIds() {
    return roles.stream().map(RoleResponse::getId).toList();
  }
}
