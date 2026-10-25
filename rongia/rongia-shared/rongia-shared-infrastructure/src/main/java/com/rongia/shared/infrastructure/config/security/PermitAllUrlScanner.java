package com.rongia.shared.infrastructure.config.security;

import jakarta.annotation.security.PermitAll;
import lombok.Getter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 扫描器：自动提取所有带 @PermitAll 注解的接口 URL
 */
@Component
@Getter
public class PermitAllUrlScanner implements InitializingBean {

    /** URL 请求映射处理器（Spring MVC 内置） */
    private final RequestMappingHandlerMapping requestMappingHandlerMapping;
    /** 存放扫描到的所有放行 URL 集合 */
    private final Set<String> permitAllUrls = new HashSet<>();

    /**
     * 构造器注入 RequestMappingHandlerMapping
     * @param requestMappingHandlerMapping Spring MVC 请求映射处理器
     */
    public PermitAllUrlScanner(RequestMappingHandlerMapping requestMappingHandlerMapping) {
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
    }

    /**
     * 项目启动后自动执行：遍历所有 Controller 方法，收集 @PermitAll 的 URL
     */
    @Override
    public void afterPropertiesSet() {
        // 获取所有 Controller 方法映射
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = requestMappingHandlerMapping.getHandlerMethods();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            HandlerMethod handlerMethod = entry.getValue();

            // 判断方法或所在类是否打了 @PermitAll
            boolean hasPermitAll = handlerMethod.hasMethodAnnotation(PermitAll.class)
                    || handlerMethod.getBeanType().isAnnotationPresent(PermitAll.class);

            if (hasPermitAll) {
                RequestMappingInfo mappingInfo = entry.getKey();
                Set<String> patterns = mappingInfo.getPatternValues();

                for (String pattern : patterns) {
                    // 将 {id}、{name} 等路径变量替换为 *（Spring Security 通配符）
                    String formattedUrl = pattern.replaceAll("\\{[^/]+?\\}", "*");
                    permitAllUrls.add(formattedUrl);
                }
            }
        }
    }
}