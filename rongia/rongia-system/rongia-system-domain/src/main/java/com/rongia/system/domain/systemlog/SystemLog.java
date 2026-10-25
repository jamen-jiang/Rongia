package com.rongia.system.domain.systemlog;

import com.rongia.shared.domain.aggregate.BaseAggregateRoot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 系统日志 */
@NullMarked
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "SystemLog",
    indexes = {
      @Index(name = "IX_SystemLog_Level", columnList = "Level"),
      @Index(name = "IX_SystemLog_TimeStamp", columnList = "TimeStamp DESC")
    })
public class SystemLog extends BaseAggregateRoot {

  /** 级别（可空） */
  @Nationalized
  @Column(name = "Level", length = 50)
  @Nullable
  private String level;

  /** 错误信息（可空)） */
  @Nationalized
  @Column(name = "Message")
  @Nullable
  private String message;

  /** 消息模板（可空） */
  @Nationalized
  @Column(name = "MessageTemplate")
  @Nullable
  private String messageTemplate;

  /** 异常信息（可空） */
  @Nationalized
  @Column(name = "Exception")
  @Nullable
  private String exception;

  /** 属性（可空） */
  @Nationalized
  @Column(name = "Properties")
  @Nullable
  private String properties;

  /** 记录时间（可空） */
  @Column(name = "TimeStamp")
  @Nullable
  private LocalDateTime timeStamp;
}
