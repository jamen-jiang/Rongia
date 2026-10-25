package com.rongia.system.application.role.command;

import com.rongia.system.domain.role.Role;
import com.rongia.system.domain.role.RoleFactory;
import com.rongia.system.domain.role.RoleRepository;
import org.axonframework.messaging.commandhandling.annotation.CommandHandler;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 添加角色命令处理器 */
@NullMarked
@Component
public class AddRoleCommandHandler {

  /** 角色工厂 */
  private final RoleFactory roleFactory;

  /** 角色仓储 */
  private final RoleRepository roleRepository;

  /**
   * 构造器注入
   *
   * @param roleFactory 角色工厂
   * @param roleRepository 角色仓储
   */
  public AddRoleCommandHandler(RoleFactory roleFactory, RoleRepository roleRepository) {
    this.roleFactory = roleFactory;
    this.roleRepository = roleRepository;
  }

  /**
   * 处理添加角色命令
   *
   * @param command 添加角色命令
   * @return 新角色Id
   */
  @CommandHandler
  @Transactional
  public Long handle(AddRoleCommand command) {
    Role model = roleFactory.create(command.getName(), command.getRemark());
    roleRepository.save(model);
    return model.getId();
  }
}
