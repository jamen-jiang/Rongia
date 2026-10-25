package com.rongia.system.application.company.command;

import com.rongia.system.domain.company.Company;
import com.rongia.system.domain.company.CompanyDomainService;
import com.rongia.system.domain.company.CompanyFactory;
import com.rongia.system.domain.company.CompanyRepository;
import org.axonframework.messaging.commandhandling.annotation.CommandHandler;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 添加公司命令处理器 */
@NullMarked
@Component
public class AddCompanyCommandHandler {

  /** 公司工厂 */
  private final CompanyFactory companyFactory;

  /** 公司领域服务 */
  private final CompanyDomainService companyDomainService;

  /** 公司仓储 */
  private final CompanyRepository companyRepository;

  /**
   * 构造器注入
   *
   * @param companyFactory 公司工厂
   * @param companyDomainService 公司领域服务
   * @param companyRepository 公司仓储
   */
  public AddCompanyCommandHandler(
      CompanyFactory companyFactory,
      CompanyDomainService companyDomainService,
      CompanyRepository companyRepository) {
    this.companyFactory = companyFactory;
    this.companyDomainService = companyDomainService;
    this.companyRepository = companyRepository;
  }

  /**
   * 处理添加公司命令
   *
   * @param command 添加公司命令
   * @return 新公司Id
   */
  @CommandHandler
  @Transactional
  public Long handle(AddCompanyCommand command) {
    // 1. 创建公司聚合（默认币别必填，为空时由领域层报错）
    Company model =
        companyFactory.create(
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

    // 2. 保存
    companyRepository.save(model);
    return model.getId();
  }
}
