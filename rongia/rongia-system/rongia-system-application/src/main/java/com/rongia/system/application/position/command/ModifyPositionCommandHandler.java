package com.rongia.system.application.position.command;

import com.rongia.shared.application.consts.EntityConst;
import com.rongia.shared.application.port.lock.DistributedLock;
import com.rongia.system.domain.position.Position;
import com.rongia.system.domain.position.PositionDomainService;
import com.rongia.system.domain.position.PositionRepository;
import java.util.List;
import org.axonframework.messaging.commandhandling.annotation.CommandHandler;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 修改岗位命令处理器 */
@NullMarked
@Component
public class ModifyPositionCommandHandler {

  /** 岗位领域服务 */
  private final PositionDomainService postDomainService;

  /** 岗位仓储 */
  private final PositionRepository postRepository;

  /**
   * 构造器注入
   *
   * @param postDomainService 岗位领域服务
   * @param postRepository 岗位仓储
   */
  public ModifyPositionCommandHandler(
      PositionDomainService postDomainService, PositionRepository postRepository) {
    this.postDomainService = postDomainService;
    this.postRepository = postRepository;
  }

  /**
   * 处理修改岗位命令
   *
   * @param command 修改岗位命令
   */
  @CommandHandler
  @Transactional
  @DistributedLock(prefix = EntityConst.POST, value = "#command.id")
  public void handle(ModifyPositionCommand command) {
    List<Position> models =
        postDomainService.modify(
            command.getId(),
            command.getName(),
            command.getDepartmentId(),
            command.getParentId(),
            command.isDisabled(),
            command.getRemark());
    for (Position model : models) {
      postRepository.save(model);
    }
  }
}
