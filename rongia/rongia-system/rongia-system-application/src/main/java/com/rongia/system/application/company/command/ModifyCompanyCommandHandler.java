package com.rongia.system.application.company.command;

import com.rongia.shared.application.consts.EntityConst;
import com.rongia.shared.application.port.lock.DistributedLock;
import com.rongia.system.domain.company.Company;
import com.rongia.system.domain.company.CompanyDomainService;
import com.rongia.system.domain.company.CompanyRepository;
import java.util.List;
import org.axonframework.messaging.commandhandling.annotation.CommandHandler;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 修改公司命令处理器 */
@NullMarked
@Component
public class ModifyCompanyCommandHandler {

  /** 公司领域服务 */
  private final CompanyDomainService companyDomainService;

  /** 公司仓储 */
  private final CompanyRepository companyRepository;

  /**
   * 构造器注入
   *
   * @param companyDomainService 公司领域服务
   * @param companyRepository 公司仓储
   */
  public ModifyCompanyCommandHandler(
      CompanyDomainService companyDomainService, CompanyRepository companyRepository) {
    this.companyDomainService = companyDomainService;
    this.companyRepository = companyRepository;
  }

  /**
   * 处理修改公司命令
   *
   * @param command 修改公司命令
   */
  @CommandHandler
  @Transactional
  @DistributedLock(prefix = EntityConst.COMPANY, value = "#command.id")
  public void handle(ModifyCompanyCommand command) {
    // 1. 通过领域服务修改公司（内部校验代码唯一 + 上级公司 + 层级编排）
    List<Company> models =
        companyDomainService.modify(
            command.getId(),
            command.getCode(),
            command.getName(),
            command.getShortName(),
            command.getParentId(),
            command.getPhone(),
            command.getEmail(),
            command.getCountry(),
            command.getRegionCode(),
            command.getRegion(),
            command.getZip(),
            command.getAddress(),
            command.getTaxRegisterCode(),
            command.getRegistryCode(),
            command.getEstablishedDate(),
            command.getCurrencyId() == null ? 0L : command.getCurrencyId(),
            command.isDisabled(),
            command.getSequence(),
            command.getRemark());

    // 2. 保存自身与受影响的子孙（层级路径可能被重排）
    for (Company model : models) {
      companyRepository.save(model);
    }
  }
}
