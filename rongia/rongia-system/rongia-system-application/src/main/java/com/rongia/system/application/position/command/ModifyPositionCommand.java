package com.rongia.system.application.position.command;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 修改岗位命令 */
@NullMarked
@Getter
@Schema(description = "修改岗位命令")
public class ModifyPositionCommand extends AddPositionCommand {

  /** 岗位Id */
  @Schema(description = "岗位Id", requiredMode = Schema.RequiredMode.REQUIRED)
  private final Long id;

  /**
   * 构造器
   *
   * @param id 岗位Id
   * @param name 岗位名称
   * @param departmentId 组织机构Id
   * @param parentId 上级岗位Id（可空）
   * @param disabled 是否禁用
   * @param remark 备注（可空）
   */
  public ModifyPositionCommand(
      long id,
      String name,
      long departmentId,
      @Nullable Long parentId,
      boolean disabled,
      @Nullable String remark) {
    super(name, departmentId, parentId, disabled, remark);
    this.id = id;
  }
}