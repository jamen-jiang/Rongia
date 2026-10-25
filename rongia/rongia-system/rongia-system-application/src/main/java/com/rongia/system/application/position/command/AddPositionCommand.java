package com.rongia.system.application.position.command;

import com.rongia.shared.application.cqrs.Command;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 添加岗位命令 */
@NullMarked
@Getter
@Schema(description = "添加岗位命令")
public class AddPositionCommand extends Command {

  /** 岗位名称 */
  @Schema(description = "岗位名称", requiredMode = Schema.RequiredMode.REQUIRED)
  private final String name;

  /** 组织机构Id */
  @Schema(description = "组织机构Id", requiredMode = Schema.RequiredMode.REQUIRED)
  private final Long departmentId;

  /** 上级岗位Id（可空） */
  @Schema(description = "上级岗位Id")
  @Nullable
  private final Long parentId;

  /** 是否禁用 */
  @Schema(description = "是否禁用", requiredMode = Schema.RequiredMode.REQUIRED)
  private final boolean disabled;

  /** 备注（可空） */
  @Schema(description = "备注")
  @Nullable
  private final String remark;

  /**
   * 构造器
   *
   * @param name 岗位名称
   * @param departmentId 组织机构Id
   * @param parentId 上级岗位Id（可空）
   * @param disabled 是否禁用
   * @param remark 备注（可空）
   */
  public AddPositionCommand(
      String name,
      long departmentId,
      @Nullable Long parentId,
      boolean disabled,
      @Nullable String remark) {
    this.name = name;
    this.departmentId = departmentId;
    this.parentId = parentId;
    this.disabled = disabled;
    this.remark = remark;
  }
}