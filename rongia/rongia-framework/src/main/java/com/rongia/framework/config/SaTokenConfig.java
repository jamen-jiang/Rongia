package com.rongia.framework.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.annotation.security.PermitAll;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 路由拦截配置：默认全部要求登录，接口打上 @PermitAll 即可免登录
 *
 * <p>免登录由注解驱动：方法或所在类标注 {@link PermitAll} 就跳过登录校验，
 * 与 Spring Security 时代的语义一致，不需要在白名单里维护业务 URL。
 * 只有静态资源（无对应 HandlerMethod）才走静态白名单。
 * 方法级鉴权后续用 @SaCheckPermission 注解，权限数据在 SaPermissionImpl。
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

  /** 静态资源白名单（没有对应 HandlerMethod，注解管不到） */
  private static final List<String> STATIC_URLS =
      List.of(
          "/v3/api-docs/**", // 以下为接口文档静态资源
          "/swagger-ui/**",
          "/swagger-ui.html",
          "/doc.html",
          "/webjars/**",
          "/favicon.ico");

  /**
   * 注册路由拦截器：@PermitAll 放行，静态白名单放行，其余一律 StpUtil.checkLogin()
   *
   * @param registry 拦截器注册器
   */
  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry
        .addInterceptor(
            new SaInterceptor(
                handle -> {
                  // 方法或所在类标注 @PermitAll 的，免登录
                  if (handle instanceof HandlerMethod handlerMethod
                      && (handlerMethod.hasMethodAnnotation(PermitAll.class)
                          || handlerMethod.getBeanType().isAnnotationPresent(PermitAll.class))) {
                    return;
                  }
                  StpUtil.checkLogin();
                }))
        .addPathPatterns("/**")
        .excludePathPatterns(STATIC_URLS);
  }
}
