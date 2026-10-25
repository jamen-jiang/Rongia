package com.rongia.shared.application.port;

import org.jspecify.annotations.NonNull;

/** 邮件发送器 */
public interface EmailSender {

  /**
   * 添加附件
   *
   * @param fileName 附件名
   * @param data 附件
   */
  @NonNull EmailSender attach(@NonNull String fileName, byte[] data);

  /**
   * 设置收件人
   *
   * @param to 收件人
   * @return this
   */
  @NonNull EmailSender to(@NonNull String to);

  /**
   * 设置抄送人
   *
   * @param cc 抄送人
   * @return this
   */
  @NonNull EmailSender cc(@NonNull String cc);

  /**
   * 设置标题
   *
   * @param subject 标题
   * @return this
   */
  @NonNull EmailSender subject(@NonNull String subject);

  /**
   * 设置内容
   *
   * @param content 内容
   * @return this
   */
  @NonNull EmailSender content(@NonNull String content);

  /** 发送 */
  void send();
}
