package com.rongia.system.domain.company;

import com.rongia.shared.domain.repository.Repository;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 公司仓储接口 */
@NullMarked
public interface CompanyRepository extends Repository<Company> {

  /**
   * 根据Id列表批量查询公司
   *
   * @param ids 公司Id列表
   * @return 公司实体列表
   */
  List<Company> getListByIds(List<Long> ids);

  /**
   * 查询自身及所有子孙公司（含自身）
   *
   * @param id 公司Id
   * @return 公司列表（含自身）
   */
  List<Company> getWithChildren(long id);

  /**
   * 按上级公司查询直接下级
   *
   * @param parentId 上级公司Id
   * @return 公司列表
   */
  List<Company> getListByParentId(long parentId);

  /**
   * 判断公司代码是否已存在（全司唯一，不分层级）
   *
   * @param code 公司代码
   * @param id 排除的Id（用于更新时校验，可为空）
   * @return 是否存在
   */
  boolean isExistByCodeExcludeId(String code, @Nullable Long id);

  /**
   * 判断公司代码是否已存在（新增时校验）
   *
   * @param code 公司代码
   * @return 是否存在
   */
  default boolean isExistByCodeExcludeId(String code) {
    return isExistByCodeExcludeId(code, null);
  }

  /**
   * 判断同一上级下公司名称是否已存在
   *
   * @param name 公司名称
   * @param parentId 上级公司Id（可空，null 表示顶级）
   * @param id 排除的Id（用于更新时校验，可为空）
   * @return 是否存在
   */
  boolean isExistByNameAndParentIdExcludeId(
      String name, @Nullable Long parentId, @Nullable Long id);

  /**
   * 判断同一上级下公司名称是否已存在（新增时校验）
   *
   * @param name 公司名称
   * @param parentId 上级公司Id（可空，null 表示顶级）
   * @return 是否存在
   */
  default boolean isExistByNameAndParentIdExcludeId(String name, @Nullable Long parentId) {
    return isExistByNameAndParentIdExcludeId(name, parentId, null);
  }
}