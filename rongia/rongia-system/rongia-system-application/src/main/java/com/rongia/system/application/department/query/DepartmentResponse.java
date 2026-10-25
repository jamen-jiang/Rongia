package com.rongia.system.application.department.query;

import com.rongia.shared.application.response.BaseResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 部门响应 */
@NullMarked
@Getter
@Setter
@Schema(description = "部门响应")
public class DepartmentResponse extends BaseResponse {

  /** 部门Id */
  @Schema(description = "部门Id", requiredMode = Schema.RequiredMode.REQUIRED)
  private Long id;

  /** 父Id（可空） */
  @Schema(description = "父Id")
  @Nullable
  private Long parentId;

  /** 父级路径（可空，形如 "1-3-5"） */
  @Schema(description = "父级路径")
  @Nullable
  private String parentPath;

  /** 公司Id */
  @Schema(description = "公司Id", requiredMode = Schema.RequiredMode.REQUIRED)
  private Long companyId;

  /** 公司名称 */
  @Schema(description = "公司名称", requiredMode = Schema.RequiredMode.REQUIRED)
  private String companyName = "";

  /** 父级部门（可空） */
  @Schema(description = "父级部门名称")
  @Nullable
  private String parentName;

  /** 部门名称 */
  @Schema(description = "部门名称", requiredMode = Schema.RequiredMode.REQUIRED)
  private String name = "";

  /** 是否禁用 */
  @Schema(description = "是否禁用", requiredMode = Schema.RequiredMode.REQUIRED)
  private boolean disabled;

  /** 排序（可空） */
  @Schema(description = "排序")
  @Nullable
  private Integer sequence;

  /** 备注（可空） */
  @Schema(description = "备注")
  @Nullable
  private String remark;
}
