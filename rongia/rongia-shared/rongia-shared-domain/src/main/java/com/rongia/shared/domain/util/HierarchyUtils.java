package com.rongia.shared.domain.util;

import com.rongia.shared.domain.entity.Parent;
import com.rongia.shared.domain.exception.DomainException;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 树形层级编排工具：只做纯计算，数据由调用方查好后传入 */
@NullMarked
public final class HierarchyUtils {

  /** 层级路径的分隔符 */
  private static final String SEPARATOR = "-";

  private HierarchyUtils() {}

  /**
   * 根据父节点计算层级路径
   *
   * <p>路径只存祖先、不含自己：父是 5、祖父是 3、曾祖是 1，这一条就是 {@code "1-3-5"}，直接挂在根下面的那一条是
   * {@code "1"}。查整棵子树时把列前后各补一个分隔符再按 {@code "-5-"} 匹配，所以这个分隔符和仓储里的条件查询是一体的，改一处必须改另一处。
   *
   * @param parentId 父节点Id（可空）
   * @param parent 父节点实体，由调用方查询后传入（可空）
   * @param label 父节点名称，用于异常信息
   * @param <T> 树形层级实体类型
   * @return 层级路径（如 "1-3-5"），无父节点返回 null（可空）
   * @throws DomainException 父节点Id非空但实体不存在时抛出
   */
  public static <T extends Parent> @Nullable String resolveParentPath(
      @Nullable Long parentId, @Nullable T parent, String label) {
    if (parentId == null) {
      return null;
    }
    if (parent == null) {
      throw new DomainException(label + "不存在: " + parentId);
    }
    return buildParentPath(parent);
  }

  /**
   * 给一批子节点重设层级路径
   *
   * @param parent 父节点
   * @param children 子节点列表，由调用方查询后传入
   * @param <T> 树形层级实体类型
   */
  public static <T extends Parent> void resetChildrenParentPath(T parent, List<T> children) {
    if (children.isEmpty()) {
      return;
    }
    String parentPath = buildParentPath(parent);
    for (T child : children) {
      child.changeParentPath(parentPath);
    }
  }

  /**
   * 拼出一个节点下面直接子节点该有的路径：父节点自己的路径（可空）后面接父Id
   *
   * @param parent 父节点
   * @param <T> 树形层级实体类型
   * @return 父级路径
   */
  private static <T extends Parent> String buildParentPath(T parent) {
    return StringUtils.isBlank(parent.getParentPath())
        ? parent.getId().toString()
        : parent.getParentPath() + SEPARATOR + parent.getId();
  }
}
