package com.rongia.shared.infrastructure.config.web;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.Locale;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.core.convert.converter.Converter;

/**
 * 宽容的 String -> LocalDateTime 转换：GET 参数（WebConfig）与 JSON body（JacksonConfig）共用同一套规则
 *
 * <p>空白文本返回 null；兼容空格或 T 作为日期与时间的分隔符、1-9 位小数秒、带时区偏移量（Z / +08:00，按 UTC 换算系统时区）
 */
@NullMarked
public final class StringToLocalDateTimeConverter
    implements Converter<String, @Nullable LocalDateTime> {

  private static final DateTimeFormatter FLEXIBLE =
      new DateTimeFormatterBuilder()
          .appendPattern("yyyy-MM-dd[' ']['T']HH:mm:ss")
          // appendFraction 支持 1-9 位小数秒（.8 / .87 / .871 均可），ofPattern 的 .SSS 是定宽表达不了
          .optionalStart()
          .appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, true)
          .optionalEnd()
          .optionalStart()
          .appendOffsetId()
          .optionalEnd()
          .toFormatter(Locale.CHINA);

  /**
   * 解析；格式非法时抛 DateTimeParseException
   *
   * @param text 待解析文本（可空）
   * @return 解析结果；空白文本返回 null
   */
  @Override
  public @Nullable LocalDateTime convert(@Nullable String text) {
    if (text == null || text.isBlank()) {
      return null;
    }
    text = text.trim();
    try {
      // 带偏移量（Z / +08:00）：按 UTC 换算系统默认时区
      return OffsetDateTime.parse(text, FLEXIBLE)
          .atZoneSameInstant(ZoneId.systemDefault())
          .toLocalDateTime();
    } catch (DateTimeParseException ignored) {
      // 纯本地时间
      return LocalDateTime.parse(text, FLEXIBLE);
    }
  }
}
