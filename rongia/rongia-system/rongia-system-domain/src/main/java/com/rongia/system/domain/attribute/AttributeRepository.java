package com.rongia.system.domain.attribute;

import com.rongia.shared.domain.repository.Repository;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 属性仓储接口 */
@NullMarked
public interface AttributeRepository extends Repository<Attribute> {

  /**
   * 按持有者类型 + 代码查询
   *
   * @param entityType 持有者表名（如 {@code Material} / {@code Variant}）
   * @param code 属性代码
   * @return 属性（可空），不存在返回 null
   */
  @Nullable Attribute getByEntityTypeAndCode(String entityType, String code);

  /**
   * 按持有者类型查询列表
   *
   * @param entityType 持有者表名（如 {@code Material} / {@code Variant}）
   * @return 属性列表
   */
  List<Attribute> getListByEntityType(String entityType);

  /**
   * 判断属性代码在持有者类型下是否已存在
   *
   * @param entityType 持有者表名（如 {@code Material} / {@code Variant}）
   * @param code 属性代码
   * @param id 排除的属性Id（可空）
   * @return true 表示已存在
   */
  boolean isExistByEntityTypeAndCodeExcludeId(String entityType, String code, @Nullable Long id);

  /**
   * 判断属性代码在持有者类型下是否已存在
   *
   * @param entityType 持有者表名（如 {@code Material} / {@code Variant}）
   * @param code 属性代码
   * @return true 表示已存在
   */
  default boolean isExistByEntityTypeAndCodeExcludeId(String entityType, String code) {
    return isExistByEntityTypeAndCodeExcludeId(entityType, code, null);
  }
}
