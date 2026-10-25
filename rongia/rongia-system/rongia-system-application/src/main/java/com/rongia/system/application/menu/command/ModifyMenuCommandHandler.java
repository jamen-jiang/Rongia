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

/** 修改菜单命令处理器 */
@NullMarked
@Component
public class ModifyMenuCommandHandler {

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
  public ModifyMenuCommandHandler(MenuDomainService menuDomainService, MenuRepository menuRepository) {
    this.menuDomainService = menuDomainService;
    this.menuRepository = menuRepository;
  }

  /**
   * 处理修改菜单命令
   *
   * @param command 修改命令
   */
  @CommandHandler
  @Transactional
  @DistributedLock(prefix = EntityConst.APP, value = "#command.id")
  public void handle(ModifyMenuCommand command) {
    // 1. 通过领域服务修改菜单（内部算 parentPath + 递归子菜单）
    List<Menu> list =
        menuDomainService.modify(
            command.getId(),
            command.getParentId(),
            command.getName(),
            command.getPath(),
            command.getComponent(),
            command.getRedirect(),
            command.getLicense(),
            command.getRemark(),
            command.getMeta().getTitle(),
            command.getMeta().getI18nKey(),
            command.getMeta().getIcon(),
            command.getMeta().getIsKeepAlive(),
            command.getMeta().getFixedIndexInTab(),
            command.getMeta().getIsMultiTab(),
            command.getMeta().getIsHide(),
            command.getMeta().getSequence());

    // 2. 保存主菜单与所有子孙（parentPath 被更新）
    for (Menu item : list) {
      menuRepository.save(item);
    }
  }
}
