package com.rongia.system.application.user.command;

import com.rongia.shared.application.consts.EntityConst;
import com.rongia.shared.application.exception.ApiException;
import com.rongia.shared.application.port.lock.DistributedLock;
import com.rongia.system.domain.user.User;
import com.rongia.system.domain.user.UserRepository;
import org.axonframework.messaging.commandhandling.annotation.CommandHandler;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 删除用户命令处理器 */
@NullMarked
@Component
public class RemoveUserCommandHandler {

  /** 用户仓储 */
  private final UserRepository userRepository;

  /**
   * 构造器注入
   *
   * @param userRepository 用户仓储
   */
  public RemoveUserCommandHandler(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /**
   * 处理删除用户命令
   *
   * @param command 删除用户命令
   */
  @CommandHandler
  @Transactional
  @DistributedLock(prefix = EntityConst.USER, value = "#command.id")
  public void handle(RemoveUserCommand command) {
    User model = userRepository.get(command.getId());
    if (model == null) {
      throw new ApiException("用户" + command.getId() + "不存在!");
    }
    model.delete();
    userRepository.save(model);
  }
}
