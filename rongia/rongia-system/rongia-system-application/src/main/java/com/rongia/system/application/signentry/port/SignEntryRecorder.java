package com.rongia.system.application.signentry.port;

import org.jspecify.annotations.NonNull;

/** 登录入口日志记录端口 */
public interface SignEntryRecorder {

  /**
   * 记录一条登录入口日志
   *
   * @param record 日志内容
   */
  void record(@NonNull SignEntryRecord record);
}
