package com.rongia.system.application.department.command;

import com.rongia.shared.application.consts.EntityConst;
import com.rongia.shared.application.port.lock.DistributedLock;
import com.rongia.system.domain.department.Department;
import com.rongia.system.domain.department.DepartmentDomainService;
import com.rongia.system.domain.department.DepartmentRepository;
import java.util.List;
import org.axonframework.messaging.commandhandling.annotation.CommandHandler;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 删除部门命令处理器 */
@NullMarked
@Component
public class RemoveDepartmentCommandHandler {

  /** 部门领域服务 */
  private final DepartmentDomainService departmentDomainService;

  /** 部门仓储 */
  private final DepartmentRepository departmentRepository;

  /**
   * 构造器注入
   *
   * @param departmentDomainService 部门领域服务
   * @param departmentRepository 部门仓储
   */
  public RemoveDepartmentCommandHandler(
      DepartmentDomainService departmentDomainService, DepartmentRepository departmentRepository) {
    this.departmentDomainService = departmentDomainService;
    this.departmentRepository = departmentRepository;
  }

  /**
   * 处理删除部门命令
   *
   * @param command 删除部门命令
   */
  @CommandHandler
  @Transactional
  @DistributedLock(prefix = EntityConst.DEPARTMENT, value = "#command.id")
  public void handle(RemoveDepartmentCommand command) {
    List<Department> list = departmentDomainService.removeWithChildren(command.getId());
    for (Department item : list) {
      departmentRepository.save(item);
    }
  }
}
