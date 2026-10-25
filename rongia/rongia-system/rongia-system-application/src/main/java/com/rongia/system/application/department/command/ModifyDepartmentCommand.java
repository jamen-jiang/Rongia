package com.rongia.system.application.department.command;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 修改部门命令 */
@NullMarked
@Getter
@Schema(description = "修改部门命令")
public class ModifyDepartmentCommand extends AddDepartmentCommand {

  /** 部门Id */
  @Schema(description = "部门Id", requiredMode = Schema.RequiredMode.REQUIRED)
  private final Long id;

  /**
   * 构造器
   *
   * @param id 部门Id
   * @param companyId 公司Id
   * @param parentId 父Id（可空）
   * @param name 组织机构名称
   * @param disabled 是否禁用
   * @param sequence 排序（可空）
   * @param remark 备注（可空）
   */
  public ModifyDepartmentCommand(
      long id,
      long companyId,
      @Nullable Long parentId,
      String name,
      boolean disabled,
      @Nullable Integer sequence,
      @Nullable String remark) {
    super(companyId, parentId, name, disabled, sequence, remark);
    this.id = id;
  }
}