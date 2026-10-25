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

/** 修改部门命令处理器 */
@NullMarked
@Component
public class ModifyDepartmentCommandHandler {

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
  public ModifyDepartmentCommandHandler(
      DepartmentDomainService departmentDomainService, DepartmentRepository departmentRepository) {
    this.departmentDomainService = departmentDomainService;
    this.departmentRepository = departmentRepository;
  }

  /**
   * 处理修改部门命令
   *
   * @param command 修改部门命令
   */
  @CommandHandler
  @Transactional
  @DistributedLock(prefix = EntityConst.DEPARTMENT, value = "#command.id")
  public void handle(ModifyDepartmentCommand command) {
    // 1. 通过领域服务修改部门（内部算 parentPath + 递归子部门）
    List<Department> list =
        departmentDomainService.modify(
            command.getId(),
            command.getCompanyId(),
            command.getParentId(),
            command.getName(),
            command.isDisabled(),
            command.getSequence(),
            command.getRemark());

    // 2. 保存主部门与所有子孙（parentPath 被更新）
    for (Department item : list) {
      departmentRepository.save(item);
    }
  }
}
