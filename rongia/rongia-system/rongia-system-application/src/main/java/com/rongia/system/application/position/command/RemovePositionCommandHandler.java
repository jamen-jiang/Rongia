package com.rongia.system.application.position.command;

import com.rongia.shared.application.consts.EntityConst;
import com.rongia.shared.application.exception.ApiException;
import com.rongia.shared.application.port.lock.DistributedLock;
import com.rongia.system.domain.position.Position;
import com.rongia.system.domain.position.PositionRepository;
import org.axonframework.messaging.commandhandling.annotation.CommandHandler;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 删除岗位命令处理器 */
@NullMarked
@Component
public class RemovePositionCommandHandler {

  /** 岗位仓储 */
  private final PositionRepository postRepository;

  /**
   * 构造器注入
   *
   * @param postRepository 岗位仓储
   */
  public RemovePositionCommandHandler(PositionRepository postRepository) {
    this.postRepository = postRepository;
  }

  /**
   * 处理删除岗位命令
   *
   * @param command 删除岗位命令
   */
  @CommandHandler
  @Transactional
  @DistributedLock(prefix = EntityConst.POST, value = "#command.id")
  public void handle(RemovePositionCommand command) {
    Position model = postRepository.get(command.getId());
    if (model == null) {
      throw new ApiException("岗位" + command.getId() + "不存在!");
    }
    model.delete();
    postRepository.save(model);
  }
}
