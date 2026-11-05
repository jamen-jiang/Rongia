package com.rongia.shared.infrastructure.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.Locale;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ext.javatime.deser.LocalDateDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalDateTimeDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalTimeDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalTimeSerializer;
import tools.jackson.databind.module.SimpleModule;

/**
 * Jackson 配置
 *
 * <p>为 Spring Boot 自动配置的 Jackson 3 {@code JsonMapper} 定制 Java 8 时间类型的 序列化/反序列化格式。
 */
@Configuration
public class JacksonConfig {

  /** 日期时间格式：yyyy-MM-dd HH:mm:ss.SSS */
  private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";

  /** 日期格式：yyyy-MM-dd */
  private static final String DATE_PATTERN = "yyyy-MM-dd";

  /** 时间格式：HH:mm:ss */
  private static final String TIME_PATTERN = "HH:mm:ss";

  /** 日期时间反序列化格式：兼容空格或 T 作为日期与时间的分隔符，毫秒段可选且支持 1-9 位 */
  private static final DateTimeFormatter FLEXIBLE_DATE_TIME_FORMATTER =
      new DateTimeFormatterBuilder()
          .appendPattern("yyyy-MM-dd[' ']['T']HH:mm:ss")
          // 使用 appendFraction 替代 [.SSS]，支持 1-9 位的小数秒（兼容 .8 / .87 / .871 等）
          .optionalStart()
          .appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, true)
          .optionalEnd()
          // 兼容带时区偏移量的格式（如 Z 或 +08:00）
          .optionalStart()
          .appendOffsetId()
          .optionalEnd()
          .toFormatter(Locale.CHINA);

  /**
   * 注册 Jackson 定制器
   *
   * @return JsonMapperBuilderCustomizer
   */
  @Bean
  public JsonMapperBuilderCustomizer jacksonCustomizer() {
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN);
    DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(TIME_PATTERN);

    SimpleModule module = new SimpleModule();

    // ---------- LocalDateTime ----------
    module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));

    // 反序列化：兼容空格 / T 分隔符，毫秒可选；带 offset 时按系统时区转换
    module.addDeserializer(
        LocalDateTime.class,
        new LocalDateTimeDeserializer(FLEXIBLE_DATE_TIME_FORMATTER) {
          @Override
          public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) {
            String text = p.getValueAsString();
            if (text == null || text.isBlank()) {
              return null;
            }
            text = text.trim();

            // 尝试按带偏移量（offset）的格式解析，例如：2026-06-08T16:00:46.87+08:00
            try {
              return OffsetDateTime.parse(text, FLEXIBLE_DATE_TIME_FORMATTER)
                  .atZoneSameInstant(ZoneId.systemDefault())
                  .toLocalDateTime();
            } catch (DateTimeParseException ignored) {
              // 如果没有 offset（纯本地时间），抛出异常后走父类默认解析
            }

            // 走父类默认解析（处理纯本地时间，例如：2026-06-08T16:00:46.87）
            return super.deserialize(p, ctxt);
          }
        });

    // ---------- LocalDate ----------
    module.addSerializer(LocalDate.class, new LocalDateSerializer(dateFormatter));
    module.addDeserializer(LocalDate.class, new LocalDateDeserializer(dateFormatter));

    // ---------- LocalTime ----------
    module.addSerializer(LocalTime.class, new LocalTimeSerializer(timeFormatter));
    module.addDeserializer(LocalTime.class, new LocalTimeDeserializer(timeFormatter));

    return builder ->
        builder
            .addModule(module)
            // 外部接口传来的是 PascalCase（CustomerCode / BulkOrderDetails / SPH），
            // DTO 字段是 camelCase，开启大小写不敏感匹配才能绑上
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES);
  }
}
