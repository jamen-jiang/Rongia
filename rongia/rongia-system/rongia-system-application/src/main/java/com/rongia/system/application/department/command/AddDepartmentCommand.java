package com.rongia.system.application.department.command;

import com.rongia.shared.application.cqrs.Command;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 添加部门命令 */
@NullMarked
@Getter
@Schema(description = "添加部门命令")
public class AddDepartmentCommand extends Command {

  /** 公司Id */
  @Schema(description = "公司Id", requiredMode = Schema.RequiredMode.REQUIRED)
  private final Long companyId;

  /** 父Id（可空） */
  @Schema(description = "父Id")
  @Nullable
  private final Long parentId;

  /** 组织机构名称 */
  @Schema(description = "组织机构名称", requiredMode = Schema.RequiredMode.REQUIRED)
  private final String name;

  /** 是否禁用 */
  @Schema(description = "是否禁用", requiredMode = Schema.RequiredMode.REQUIRED)
  private final boolean disabled;

  /** 排序（可空） */
  @Schema(description = "排序")
  @Nullable
  private final Integer sequence;

  /** 备注（可空） */
  @Schema(description = "备注")
  @Nullable
  private final String remark;

  /**
   * 构造器
   *
   * @param companyId 公司Id
   * @param parentId 父Id（可空）
   * @param name 组织机构名称
   * @param disabled 是否禁用
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   */
  public AddDepartmentCommand(
      long companyId,
      @Nullable Long parentId,
      String name,
      boolean disabled,
      @Nullable Integer sequence,
      @Nullable String remark) {
    this.companyId = companyId;
    this.parentId = parentId;
    this.name = name;
    this.disabled = disabled;
    this.sequence = sequence;
    this.remark = remark;
  }
}