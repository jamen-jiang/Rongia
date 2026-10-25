package com.rongia.system.application.user.command;

import com.rongia.shared.application.consts.EntityConst;
import com.rongia.shared.application.port.lock.DistributedLock;
import com.rongia.system.domain.user.User;
import com.rongia.system.domain.user.UserDomainService;
import com.rongia.system.domain.user.UserRepository;
import org.axonframework.messaging.commandhandling.annotation.CommandHandler;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 修改用户命令处理器 */
@NullMarked
@Component
public class ModifyUserCommandHandler {

  /** 用户领域服务 */
  private final UserDomainService userDomainService;

  /** 用户仓储 */
  private final UserRepository userRepository;

  /**
   * 构造器注入
   *
   * @param userDomainService 用户领域服务
   * @param userRepository 用户仓储
   */
  public ModifyUserCommandHandler(
      UserDomainService userDomainService, UserRepository userRepository) {
    this.userDomainService = userDomainService;
    this.userRepository = userRepository;
  }

  /**
   * 处理修改用户命令
   *
   * @param command 修改用户命令
   */
  @CommandHandler
  @Transactional
  @DistributedLock(prefix = EntityConst.USER, value = "#command.id")
  public void handle(ModifyUserCommand command) {
    User model =
        userDomainService.modify(
            command.getId(),
            command.getName(),
            command.getUserName(),
            command.getEmail(),
            command.getMobile(),
            command.getPassword(),
            command.isDisabled(),
            command.getRemark(),
            command.getRoleIds());
    userRepository.save(model);
  }
}
