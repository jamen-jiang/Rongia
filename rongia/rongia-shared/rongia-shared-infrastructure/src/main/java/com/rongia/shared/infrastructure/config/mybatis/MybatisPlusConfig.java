package com.rongia.shared.infrastructure.config.mybatis;

import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import java.util.TreeMap;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** MybatisPlus 配置 */
@Configuration
@MapperScan("com.rongia.**.infrastructure.**.mapper") // 扫描 infrastructure 下所有 @Mapper
public class MybatisPlusConfig {

  /**
   * MyBatis-Plus 插件配置
   *
   * @return MybatisPlusInterceptor
   */
  @Bean
  public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    // 防全表更新删除插件
    // interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
    // 使用自定义的 SQL Server 分页拦截器
    PaginationInnerInterceptor paginationInterceptor = new SqlServerPaginationInterceptor();
    interceptor.addInnerInterceptor(paginationInterceptor);
    return interceptor;
  }

  /** NoLock 拦截器：放在 MybatisPlus 之后注册，确保它在分页 SQL 之上再加 NOLOCK */
  @Bean
  public ConfigurationCustomizer noLockCustomizer() {
    return configuration -> configuration.addInterceptor(new NoLockInterceptor());
  }

  /**
   * 全局注册 JSON 属性 TypeHandler
   *
   * <p>同时覆盖 {@code TreeMap} 和 {@code Map} 两种字段类型，避免每次写 typeHandler
   *
   * @return ConfigurationCustomizer
   */
  @Bean
  public ConfigurationCustomizer jsonMapTypeHandlerCustomizer() {
    return configuration -> {
      var registry = configuration.getTypeHandlerRegistry();
      var handler = new TreeMapTypeHandler();
      registry.register(TreeMap.class, handler);
    };
  }
}
