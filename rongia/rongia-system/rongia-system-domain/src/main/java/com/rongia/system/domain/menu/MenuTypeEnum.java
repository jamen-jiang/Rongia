package com.rongia.system.domain.menu;

import com.rongia.shared.domain.enums.BaseEnum;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/** 菜单类型 */
@NullMarked
@Getter
public enum MenuTypeEnum implements BaseEnum {

  /** 目录 */
  CATALOG("Catalog", "目录"),

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
  MenuTypeEnum(String code, String label) {
    this.code = code;
    this.label = label;
  }
}
