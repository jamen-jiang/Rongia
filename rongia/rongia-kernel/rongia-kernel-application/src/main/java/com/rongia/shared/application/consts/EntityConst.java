package com.rongia.shared.application.consts;

import org.jspecify.annotations.NullMarked;

/** 实体名称常量（用于分布式锁前缀、日志分类等） */
@NullMarked
public final class EntityConst {

  private EntityConst() {}

  /** 应用 */
  public static final String APP = "app";

  /** 部门 */
  public static final String DEPARTMENT = "department";

  /** 岗位 */
  public static final String POST = "post";

  /** 角色 */
  public static final String ROLE = "role";

  /** 用户 */
  public static final String USER = "user";

  /** 公司 */
  public static final String COMPANY = "company";
}
