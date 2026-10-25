package com.rongia.system.application.position.command;

import com.rongia.system.domain.position.Position;
import com.rongia.system.domain.position.PositionFactory;
import com.rongia.system.domain.position.PositionRepository;
import org.axonframework.messaging.commandhandling.annotation.CommandHandler;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 添加岗位命令处理器 */
@NullMarked
@Component
public class AddPositionCommandHandler {

  /** 岗位工厂 */
  private final PositionFactory postFactory;

  /** 岗位仓储 */
  private final PositionRepository postRepository;

  /**
   * 构造器注入
   *
   * @param postFactory 岗位工厂
   * @param postRepository 岗位仓储
   */
  public AddPositionCommandHandler(PositionFactory postFactory, PositionRepository postRepository) {
    this.postFactory = postFactory;
    this.postRepository = postRepository;
  }

  /**
   * 处理添加岗位命令
   *
   * @param command 添加岗位命令
   * @return 新岗位Id
   */
  @CommandHandler
  @Transactional
  public Long handle(AddPositionCommand command) {
    Position model =
        postFactory.create(
            command.getName(),
            command.getDepartmentId(),
            command.getParentId(),
            command.isDisabled(),
            command.getRemark());
    postRepository.save(model);
    return model.getId();
  }
}
