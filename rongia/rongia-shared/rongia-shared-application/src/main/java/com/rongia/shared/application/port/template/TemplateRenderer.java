package com.rongia.shared.application.port.template;

import java.util.Map;
import org.jspecify.annotations.NullMarked;

/** 模板渲染端口 */
@NullMarked
public interface TemplateRenderer {

  /**
   * 渲染模板
   *
   * @param template 模板字符串
   * @param variables 变量
   * @return 渲染结果
   */
  String render(String template, Map<String, Object> variables);
}
