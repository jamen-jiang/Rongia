package com.rongia.system.application.menu.command;

import com.rongia.system.domain.menu.Menu;
import com.rongia.system.domain.menu.MenuFactory;
import com.rongia.system.domain.menu.MenuRepository;
import org.axonframework.messaging.commandhandling.annotation.CommandHandler;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 创建菜单命令处理器 */
@NullMarked
@Component
public class AddMenuCommandHandler {

  /** 菜单工厂 */
  private final MenuFactory menuFactory;

  /** 菜单仓储 */
  private final MenuRepository menuRepository;

  /**
   * 构造器注入
   *
   * @param menuFactory 菜单工厂
   * @param menuRepository 菜单仓储
   */
  public AddMenuCommandHandler(MenuFactory menuFactory, MenuRepository menuRepository) {
    this.menuFactory = menuFactory;
    this.menuRepository = menuRepository;
  }

  /**
   * 处理创建菜单命令
   *
   * @param command 创建命令
   * @return 新菜单Id
   */
  @CommandHandler
  @Transactional
  public Long handle(AddMenuCommand command) {
    Menu model =
        menuFactory.create(
            command.getType(),
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
    menuRepository.save(model);
    return model.getId();
  }
}
