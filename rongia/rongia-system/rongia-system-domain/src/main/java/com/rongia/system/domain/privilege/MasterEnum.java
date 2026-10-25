package com.rongia.system.domain.privilege;

import com.rongia.shared.domain.enums.BaseEnum;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/** 权限对象 */
@NullMarked
@Getter
public enum MasterEnum implements BaseEnum {

  /** 角色 */
  ROLE("Role", "角色"),

  /** 用户 */
  USER("User", "用户"),

  /** 部门 */
  DEPARTMENT("Department", "部门"),

  /** 岗位 */
  POSITION("Position", "岗位");

  /** 编码 */
  private final String code;

  /** 标签 */
  private final String label;

  /**
   * 构造器
   *
   * @param code 编码
   * @param label 标签
   */
  MasterEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }
}
