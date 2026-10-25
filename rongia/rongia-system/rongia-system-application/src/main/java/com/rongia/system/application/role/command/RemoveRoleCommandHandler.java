package com.rongia.system.application.role.command;

import com.rongia.shared.application.consts.EntityConst;
import com.rongia.shared.application.exception.ApiException;
import com.rongia.shared.application.port.lock.DistributedLock;
import com.rongia.system.domain.role.Role;
import com.rongia.system.domain.role.RoleRepository;
import org.axonframework.messaging.commandhandling.annotation.CommandHandler;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 删除角色命令处理器 */
@NullMarked
@Component
public class RemoveRoleCommandHandler {

  /** 角色仓储 */
  private final RoleRepository roleRepository;

  /**
   * 构造器注入
   *
   * @param roleRepository 角色仓储
   */
  public RemoveRoleCommandHandler(RoleRepository roleRepository) {
    this.roleRepository = roleRepository;
  }

  /**
   * 处理删除角色命令
   *
   * @param command 删除角色命令
   */
  @CommandHandler
  @Transactional
  @DistributedLock(prefix = EntityConst.ROLE, value = "#command.id")
  public void handle(RemoveRoleCommand command) {
    Role model = roleRepository.get(command.getId());
    if (model == null) {
      throw new ApiException("角色" + command.getId() + "不存在!");
    }
    model.delete();
    roleRepository.save(model);
  }
}
