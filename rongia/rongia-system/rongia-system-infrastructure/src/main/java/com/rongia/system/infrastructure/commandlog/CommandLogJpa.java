package com.rongia.system.infrastructure.commandlog;

import com.rongia.system.domain.commandlog.CommandLog;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/** 命令日志 JPA 接口 */
@NullMarked
@Repository
public interface CommandLogJpa
    extends JpaRepository<CommandLog, Long>, JpaSpecificationExecutor<CommandLog> {}
