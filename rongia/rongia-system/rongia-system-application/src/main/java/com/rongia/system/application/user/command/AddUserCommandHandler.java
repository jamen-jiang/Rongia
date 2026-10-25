package com.rongia.system.application.user.command;

import com.rongia.system.domain.user.User;
import com.rongia.system.domain.user.UserFactory;
import com.rongia.system.domain.user.UserRepository;
import org.axonframework.messaging.commandhandling.annotation.CommandHandler;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 添加用户命令处理器 */
@NullMarked
@Component
public class AddUserCommandHandler {

  /** 用户工厂 */
  private final UserFactory userFactory;

  /** 用户仓储 */
  private final UserRepository userRepository;

  /**
   * 构造器注入
   *
   * @param userFactory 用户工厂
   * @param userRepository 用户仓储
   */
  public AddUserCommandHandler(UserFactory userFactory, UserRepository userRepository) {
    this.userFactory = userFactory;
    this.userRepository = userRepository;
  }

  /**
   * 处理添加用户命令
   *
   * @param command 添加用户命令
   * @return 新用户Id
   */
  @CommandHandler
  @Transactional
  public Long handle(AddUserCommand command) {
    User model =
        userFactory.create(
            command.getName(),
            command.getUserName(),
            command.getEmail(),
            command.getMobile(),
            command.getPassword(),
            command.isDisabled(),
            command.getRemark(),
            command.getRoleIds());
    userRepository.save(model);
    return model.getId();
  }
}
