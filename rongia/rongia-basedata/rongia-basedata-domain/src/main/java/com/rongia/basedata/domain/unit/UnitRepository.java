package com.rongia.basedata.domain.unit;

import com.rongia.shared.domain.repository.Repository;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 单位仓储接口 */
@NullMarked
public interface UnitRepository extends Repository<Unit> {

  /**
   * 按代码查询
   *
   * @param code 单位代码
   * @return 单位（可空），不存在返回 null
   */
  @Nullable Unit getByCode(String code);

  /**
   * 按参考单位查询下级单位
   *
   * @param parentId 参考单位Id
   * @return 下级单位列表
   */
  List<Unit> getListByParentId(long parentId);

  /**
   * 判断单位代码是否存在
   *
   * @param code 单位代码
   * @param id 排除的单位Id（可空，新增时传 null）
   * @return true 表示已存在
   */
  boolean isExistByCodeExcludeId(String code, @Nullable Long id);

  /**
   * 判断单位代码是否存在
   *
   * @param code 单位代码
   * @return true 表示已存在
   */
  default boolean isExistByCodeExcludeId(String code) {
    return isExistByCodeExcludeId(code, null);
  }
}
