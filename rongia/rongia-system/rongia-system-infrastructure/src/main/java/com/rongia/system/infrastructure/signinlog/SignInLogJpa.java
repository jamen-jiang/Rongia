package com.rongia.system.infrastructure.signinlog;

import com.rongia.system.domain.signinlog.SignInLog;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/** 登录日志 JPA 接口 */
@NullMarked
@Repository
public interface SignInLogJpa
    extends JpaRepository<SignInLog, Long>, JpaSpecificationExecutor<SignInLog> {}
