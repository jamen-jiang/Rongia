package com.rongia.system.domain.privilege;

import com.rongia.shared.domain.enums.BaseEnum;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/** 权限通道 */
@NullMarked
@Getter
public enum AccessEnum implements BaseEnum {

  /** 菜单 */
  MENU("Menu", "菜单"),

  /** 资源 */
  RESOURCE("Resource", "资源");

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
  AccessEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }
}
