package com.rongia.system.infrastructure.commandlog;

import com.rongia.system.domain.commandlog.CommandLog;
import com.rongia.system.domain.commandlog.CommandLogRepository;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

/** 命令日志仓储实现 */
@NullMarked
@Repository
public class CommandLogRepositoryImpl implements CommandLogRepository {

  /** JPA 接口 */
  private final CommandLogJpa jpa;

  /**
   * 构造器注入
   *
   * @param jpa JPA 接口
   */
  public CommandLogRepositoryImpl(CommandLogJpa jpa) {
    this.jpa = jpa;
  }

  /**
   * 按主键查询
   *
   * @param id 主键
   * @return 命令日志，不存在返回 null
   */
  @Override
  @Nullable
  public CommandLog get(long id) {
    return jpa.findById(id).orElse(null);
  }

  /**
   * 保存（新增或更新）
   *
   * @param model 命令日志
   */
  @Override
  public void save(CommandLog model) {
    jpa.save(model);
  }
}
