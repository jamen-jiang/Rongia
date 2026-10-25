package com.rongia.system.application.role.command;

import com.rongia.shared.application.consts.EntityConst;
import com.rongia.shared.application.port.lock.DistributedLock;
import com.rongia.system.domain.role.Role;
import com.rongia.system.domain.role.RoleDomainService;
import com.rongia.system.domain.role.RoleRepository;
import org.axonframework.messaging.commandhandling.annotation.CommandHandler;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 修改角色命令处理器 */
@NullMarked
@Component
public class ModifyRoleCommandHandler {

  /** 角色领域服务 */
  private final RoleDomainService roleDomainService;

  /** 角色仓储 */
  private final RoleRepository roleRepository;

  /**
   * 构造器注入
   *
   * @param roleDomainService 角色领域服务
   * @param roleRepository 角色仓储
   */
  public ModifyRoleCommandHandler(
      RoleDomainService roleDomainService, RoleRepository roleRepository) {
    this.roleDomainService = roleDomainService;
    this.roleRepository = roleRepository;
  }

  /**
   * 处理修改角色命令
   *
   * @param command 修改角色命令
   */
  @CommandHandler
  @Transactional
  @DistributedLock(prefix = EntityConst.ROLE, value = "#command.id")
  public void handle(ModifyRoleCommand command) {
    Role model = roleDomainService.modify(command.getId(), command.getName(), command.getRemark());
    roleRepository.save(model);
  }
}
