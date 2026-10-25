package com.rongia.system.domain.position;

import com.rongia.shared.domain.exception.DomainException;
import com.rongia.shared.domain.util.HierarchyUtils;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 岗位工厂，负责创建岗位 */
@NullMarked
public class PositionFactory {

  /** 岗位仓储 */
  private final PositionRepository postRepository;

  /**
   * 构造器注入
   *
   * @param postRepository 岗位仓储
   */
  public PositionFactory(PositionRepository postRepository) {
    this.postRepository = postRepository;
  }

  /**
   * 创建岗位
   *
   * @param name 岗位名称
   * @param departmentId 部门Id
   * @param parentId 上级岗位Id（可空）
   * @param disabled 是否禁用
   * @param remark 备注（可空）
   * @return 岗位聚合
   * @throws DomainException 岗位名称已存在、或上级岗位不存在时抛出
   */
  public Position create(
      String name,
      long departmentId,
      @Nullable Long parentId,
      boolean disabled,
      @Nullable String remark) {
    // 校验岗位名称唯一
    if (postRepository.isExistByNameExcludeId(name)) {
      throw new DomainException("岗位" + name + "已存在!");
    }
    return new Position(
        name,
        departmentId,
        parentId,
        HierarchyUtils.resolveParentPath(parentId, getParent(parentId), "上级岗位"),
        disabled,
        remark);
  }

  /**
   * 查询上级岗位
   *
   * @param parentId 上级岗位Id（可空）
   * @return 上级岗位，无父Id或不存在时返回 null（可空）
   */
  @Nullable
  private Position getParent(@Nullable Long parentId) {
    return parentId == null ? null : postRepository.get(parentId);
  }
}