package com.rongia.shared.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

/** Thymeleaf 配置：字符串模板引擎 */
@Configuration
public class ThymeleafConfig {

  /**
   * 字符串模板引擎
   *
   * <p>必须用 {@link SpringTemplateEngine}（SpringEL）而不是原生 {@code TemplateEngine}： {@code
   * thymeleaf-spring6} 的 POM 已显式排除 {@code ognl}，而原生引擎的 StandardDialect 解析 {@code ${...}} 依赖 OGNL，会抛
   * {@code NoClassDefFoundError: ognl/PropertyAccessor}。
   *
   * @return 模板引擎
   */
  @Bean
  public TemplateEngine stringTemplateEngine() {
    StringTemplateResolver resolver = new StringTemplateResolver();
    resolver.setTemplateMode(TemplateMode.HTML);
    resolver.setCacheable(false);

    SpringTemplateEngine engine = new SpringTemplateEngine();
    engine.setTemplateResolver(resolver);
    return engine;
  }
}
