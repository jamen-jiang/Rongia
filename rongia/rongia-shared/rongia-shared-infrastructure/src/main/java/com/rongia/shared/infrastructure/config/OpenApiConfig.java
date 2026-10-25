package com.rongia.shared.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI 配置 */
@Configuration
public class OpenApiConfig {

  /** 认证方案名称 */
  private static final String SECURITY_SCHEME_NAME = "Bearer";

  /**
   * OpenAPI 配置
   *
   * @return OpenAPI
   */
  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI()
        .info(new Info().title("RONGIA API").description("RONGIA 接口文档").version("1.0.0"))
        .components(
            new Components()
                .addSecuritySchemes(
                    SECURITY_SCHEME_NAME,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .in(SecurityScheme.In.HEADER)
                        .name("Authorization")))
        // 全局应用，所有接口默认都需要带 token
        .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
  }
}
