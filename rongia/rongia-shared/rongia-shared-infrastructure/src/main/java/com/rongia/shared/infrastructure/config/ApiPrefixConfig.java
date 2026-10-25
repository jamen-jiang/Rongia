package com.rongia.shared.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 只给 com.rongia 包下 @RestController 加 /api 前缀，SpringDoc / Knife4j 不受影响 */
@Configuration
public class ApiPrefixConfig implements WebMvcConfigurer {

  /**
   * 配置路径前缀
   *
   * @param configurer 路径匹配配置器
   */
  @Override
  public void configurePathMatch(PathMatchConfigurer configurer) {
    configurer.addPathPrefix(
        "/api",
        HandlerTypePredicate.forAnnotation(RestController.class)
            .and(HandlerTypePredicate.forBasePackage("com.rongia")));
  }
}
