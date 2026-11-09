package com.rongia.platform.web;

import java.time.LocalDateTime;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Web 配置：Vue Router History 模式刷新转发 + GET 参数 String -> LocalDateTime 转换 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    // 将所有不带 "." 的路径（即非静态资源请求）统统转发给 index.html，交给 Vue Router 处理
    // 例如 /api/user/list 刷新时，会返回 index.html
    registry.addViewController("/{path:[^\\\\\\\\.]*}").setViewName("forward:/index.html");

    // 如果你的 Vue 路由有二级路径，比如 /api/user/detail/1，需要再加一条
    registry.addViewController("/**/{path:[^\\\\\\\\.]*}").setViewName("forward:/index.html");
  }

  @Override
  public void addFormatters(FormatterRegistry registry) {
    // 宽容解析规则与 JSON body 一致：见 StringToLocalDateTimeConverter
    registry.addConverter(String.class, LocalDateTime.class, new StringToLocalDateTimeConverter());
  }
}
