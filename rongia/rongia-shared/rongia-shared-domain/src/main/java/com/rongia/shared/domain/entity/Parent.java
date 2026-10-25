package com.rongia.shared.domain.entity;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 树形层级实体：拥有父节点与层级路径。
 *
 * <p>列映射（ParentId / ParentPath）由各实现类自己的字段注解决定 —— JPA 不支持在接口方法上定义映射，因此此处不写
 * {@code @Column} 等注解。
 */
@NullMarked
public interface Parent extends Entity {

  /**
   * 父节点Id
   *
   * @return 父节点Id，根节点为 null（可空）
   */
  @Nullable Long getParentId();

  /**
   * 层级路径
   *
   * @return 层级路径（如 "1-3-5"），根节点为 null（可空）
   */
  @Nullable String getParentPath();

  /**
   * 重设层级路径（由层级编排逻辑调用）
   *
   * @param parentPath 层级路径（可空）
   */
  void changeParentPath(@Nullable String parentPath);
}
