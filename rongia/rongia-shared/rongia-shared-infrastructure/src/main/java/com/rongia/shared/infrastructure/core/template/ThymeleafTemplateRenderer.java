package com.rongia.shared.infrastructure.core.template;

import com.rongia.shared.application.port.template.TemplateRenderer;
import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/** 模板渲染端口实现：基于 Thymeleaf，渲染字符串模板（如邮件正文） */
@NullMarked
@Component
public class ThymeleafTemplateRenderer implements TemplateRenderer {

  /** 字符串模板引擎（由 ThymeleafConfig 装配） */
  private final TemplateEngine templateEngine;

  /**
   * 构造器注入
   *
   * @param templateEngine 字符串模板引擎
   */
  public ThymeleafTemplateRenderer(
      @Qualifier("stringTemplateEngine") TemplateEngine templateEngine) {
    this.templateEngine = templateEngine;
  }

  /**
   * 渲染模板字符串
   *
   * <p>模板里可用 Thymeleaf 标准语法，如 {@code th:text="${currentDate}"}。
   *
   * @param template HTML 模板字符串
   * @param variables 模板变量
   * @return 渲染后的 HTML
   */
  @Override
  public String render(String template, Map<String, Object> variables) {
    Context context = new Context();
    context.setVariables(variables);
    return templateEngine.process(template, context);
  }
}
