package com.rongia.system.domain.position;

import com.rongia.shared.domain.exception.DomainException;
import com.rongia.shared.domain.util.HierarchyUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 岗位领域服务，负责层级编排 */
@NullMarked
public class PositionDomainService {

  /** 岗位仓储 */
  private final PositionRepository postRepository;

  /**
   * 构造器注入
   *
   * @param postRepository 岗位仓储
   */
  public PositionDomainService(PositionRepository postRepository) {

    this.postRepository = postRepository;
  }

  /**
   * 修改岗位（含层级编排）
   *
   * @param id 岗位Id
   * @param name 岗位名称
   * @param departmentId 部门Id
   * @param parentId 上级岗位Id（可空）
   * @param disabled 是否禁用
   * @param remark 备注（可空）
   * @return 需要持久化的聚合列表（自身 + 受影响的子孙）
   * @throws DomainException 岗位不存在、岗位名称已存在、或上级岗位是自己/自己的子孙时抛出
   */
  public List<Position> modify(
      long id,
      String name,
      long departmentId,
      @Nullable Long parentId,
      boolean disabled,
      @Nullable String remark) {
    // 1. 查岗位
    Position model = postRepository.get(id);
    if (model == null) {
      throw new DomainException("岗位不存在!");
    }
    // 2. 校验岗位名称唯一（排除自身）
    if (postRepository.isExistByNameExcludeId(name, id)) {
      throw new DomainException("岗位" + name + "已存在!");
    }
    // 3. 校验上级岗位并重排层级
    Position parent = getParent(parentId);
    validateParent(model, parentId, parent);
    model.modify(
        name,
        departmentId,
        parentId,
        HierarchyUtils.resolveParentPath(parentId, parent, "上级岗位"),
        disabled,
        remark);
    List<Position> list = new ArrayList<>();
    setChildrenParentPath(model, list);
    list.addFirst(model);
    return list;
  }

  /**
   * 删除自身及所有子孙岗位
   *
   * @param id 岗位Id
   * @return 需要持久化的聚合列表（已标记删除）
   */
  public List<Position> removeWithChildren(long id) {
    List<Position> list = postRepository.getWithChildren(id);
    for (Position item : list) {
      item.delete();
    }
    return list;
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

  /**
   * 校验上级岗位合法
   *
   * <p>不能是自己、不能是自己的子孙——不拦的话层级路径会绕成一个环，"整棵子树"的查询和删除都会跟着出错。
   * 与 {@code Category}、{@code Department}、{@code Company} 同一套规矩。
   *
   * @param model 岗位
   * @param parentId 上级岗位Id（可空）
   * @param parent 上级岗位（可空）
   * @throws DomainException 上级岗位是自己或自己的子孙时抛出
   */
  private void validateParent(Position model, @Nullable Long parentId, @Nullable Position parent) {
    if (parentId == null || parent == null) {
      return;
    }
    if (Objects.equals(parentId, model.getId())) {
      throw new DomainException("上级岗位不能是自己!");
    }
    boolean isDescendant =
        postRepository.getWithChildren(model.getId()).stream()
            .anyMatch(item -> Objects.equals(item.getId(), parentId));
    if (isDescendant) {
      throw new DomainException("上级岗位不能是自己的子孙岗位!");
    }
  }

  /**
   * 递归设置下级岗位层级
   *
   * @param model 岗位
   * @param list 下级岗位收集列表
   */
  private void setChildrenParentPath(Position model, List<Position> list) {
    if (model.getId() == 0) {
      return;
    }
    List<Position> children = postRepository.getListByParentId(model.getId());
    HierarchyUtils.resetChildrenParentPath(model, children);
    for (Position child : children) {
      setChildrenParentPath(child, list);
      list.add(child);
    }
  }
}