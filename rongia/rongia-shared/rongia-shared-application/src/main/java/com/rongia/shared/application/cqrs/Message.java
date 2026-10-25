package com.rongia.shared.application.cqrs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;

/** 消息基类 */
@NullMarked
@Getter
public abstract class Message {

  /** 消息类型（类名） */
  @JsonIgnore private final String messageType;

  /** 时间戳 */
  @JsonIgnore private final LocalDateTime timestamp;

  /** 构造器，自动设置消息类型和时间戳 */
  protected Message() {
    this.messageType = getClass().getSimpleName();
    this.timestamp = LocalDateTime.now();
  }
}
