package com.rongia.shared.infrastructure.config.web;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * String -> LocalDateTime 转换器。 空串返回 null；兼容 {@code 2026-09-20T00:00:00.000Z}（UTC）和 {@code
 * 2026-09-20T00:00:00}（本地）。
 */
@Component
public class StringToLocalDateTimeConverter implements Converter<String, LocalDateTime> {

  @Override
  @Nullable
  public LocalDateTime convert(@NonNull String source) {
    // 空串 -> null，避免前端传 deliveryOnStart= 时报 typeMismatch
    if (source.isBlank()) {
      return null;
    }
    // 带 Z：按 UTC 解析后转系统默认时区
    if (source.endsWith("Z")) {
      return Instant.parse(source).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
    // 不带时区：按 ISO 本地时间解析
    return LocalDateTime.parse(source);
  }
}
