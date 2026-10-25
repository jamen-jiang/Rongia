package com.rongia.system.application.position.query;

import com.rongia.shared.application.response.BaseResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 岗位响应 */
@NullMarked
@Getter
@Setter
@Schema(description = "岗位响应")
public class PositionResponse extends BaseResponse {

  /** 岗位Id */
  @Schema(description = "岗位Id", requiredMode = Schema.RequiredMode.REQUIRED)
  private Long id;

  /** 上级岗位Id（可空） */
  @Schema(description = "上级岗位Id")
  @Nullable
  private Long parentId;

  /** 层级路径（可空） */
  @Schema(description = "层级路径")
  @Nullable
  private String parentPath;

  /** 岗位名称 */
  @Schema(description = "岗位名称", requiredMode = Schema.RequiredMode.REQUIRED)
  private String name = "";

  /** 公司Id */
  @Schema(description = "公司Id", requiredMode = Schema.RequiredMode.REQUIRED)
  private Long companyId = 0L;

  /** 公司名称 */
  @Schema(description = "公司名称", requiredMode = Schema.RequiredMode.REQUIRED)
  private String companyName = "";

  /** 部门Id */
  @Schema(description = "部门Id", requiredMode = Schema.RequiredMode.REQUIRED)
  private Long departmentId = 0L;

  /** 部门名称 */
  @Schema(description = "部门名称", requiredMode = Schema.RequiredMode.REQUIRED)
  private String departmentName = "";

  /** 是否禁用 */
  @Schema(description = "是否禁用", requiredMode = Schema.RequiredMode.REQUIRED)
  private boolean disabled;

  /** 备注（可空） */
  @Schema(description = "备注")
  @Nullable
  private String remark;
}
