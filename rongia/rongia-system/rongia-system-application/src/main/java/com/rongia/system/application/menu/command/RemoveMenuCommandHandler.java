package com.rongia.system.application.menu.command;

import com.rongia.shared.application.consts.EntityConst;
import com.rongia.shared.application.port.lock.DistributedLock;
import com.rongia.system.domain.menu.Menu;
import com.rongia.system.domain.menu.MenuDomainService;
import com.rongia.system.domain.menu.MenuRepository;
import java.util.List;
import org.axonframework.messaging.commandhandling.annotation.CommandHandler;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 删除菜单命令处理器 */
@NullMarked
@Component
public class RemoveMenuCommandHandler {

  /** 菜单领域服务 */
  private final MenuDomainService menuDomainService;

  /** 菜单仓储 */
  private final MenuRepository menuRepository;

  /**
   * 构造器注入
   *
   * @param menuDomainService 菜单领域服务
   * @param menuRepository 菜单仓储
   */
  public RemoveMenuCommandHandler(MenuDomainService menuDomainService, MenuRepository menuRepository) {
    this.menuDomainService = menuDomainService;
    this.menuRepository = menuRepository;
  }

  /**
   * 处理删除菜单命令
   *
   * @param command 删除菜单命令
   */
  @CommandHandler
  @Transactional
  @DistributedLock(prefix = EntityConst.APP, value = "#command.id")
  public void handle(RemoveMenuCommand command) {
    List<Menu> list = menuDomainService.removeWithChildren(command.getId());
    for (Menu item : list) {
      menuRepository.save(item);
    }
  }
}
