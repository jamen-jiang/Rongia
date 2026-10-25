package com.rongia.system.application.department.command;

import com.rongia.system.domain.department.Department;
import com.rongia.system.domain.department.DepartmentFactory;
import com.rongia.system.domain.department.DepartmentRepository;
import org.axonframework.messaging.commandhandling.annotation.CommandHandler;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 添加部门命令处理器 */
@NullMarked
@Component
public class AddDepartmentCommandHandler {

  /** 部门工厂 */
  private final DepartmentFactory departmentFactory;

  /** 部门仓储 */
  private final DepartmentRepository departmentRepository;

  /**
   * 构造器注入
   *
   * @param departmentFactory 部门工厂
   * @param departmentRepository 部门仓储
   */
  public AddDepartmentCommandHandler(
      DepartmentFactory departmentFactory, DepartmentRepository departmentRepository) {
    this.departmentFactory = departmentFactory;
    this.departmentRepository = departmentRepository;
  }

  /**
   * 处理添加部门命令
   *
   * @param command 添加部门命令
   * @return 新部门Id
   */
  @CommandHandler
  @Transactional
  public Long handle(AddDepartmentCommand command) {
    Department model =
        departmentFactory.create(
            command.getCompanyId(),
            command.getParentId(),
            command.getName(),
            command.isDisabled(),
            command.getSequence(),
            command.getRemark());
    departmentRepository.save(model);
    return model.getId();
  }
}
