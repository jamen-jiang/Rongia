package com.rongia.system.infrastructure.signinlog;

import com.rongia.system.domain.signinlog.SignInLog;
import com.rongia.system.domain.signinlog.SignInLogRepository;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

/** 登录日志仓储实现 */
@NullMarked
@Repository
public class SignInLogRepositoryImpl implements SignInLogRepository {

  /** JPA 接口 */
  private final SignInLogJpa jpa;

  /**
   * 构造器注入
   *
   * @param jpa JPA 接口
   */
  public SignInLogRepositoryImpl(SignInLogJpa jpa) {
    this.jpa = jpa;
  }

  /**
   * 按主键查询
   *
   * @param id 主键
   * @return 登录日志，不存在返回 null
   */
  @Override
  @Nullable
  public SignInLog get(long id) {
    return jpa.findById(id).orElse(null);
  }

  /**
   * 保存（新增或更新）
   *
   * @param model 登录日志
   */
  @Override
  public void save(SignInLog model) {
    jpa.save(model);
  }
}
