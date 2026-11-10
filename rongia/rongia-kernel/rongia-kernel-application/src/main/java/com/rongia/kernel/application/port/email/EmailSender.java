package com.rongia.kernel.application.port.email;

import org.jspecify.annotations.NullMarked;

/** 邮件发送器 */
@NullMarked
public interface EmailSender {

  /**
   * 添加附件
   *
   * @param fileName 附件名
   * @param data 附件
   */
  EmailSender attach(String fileName, byte[] data);

  /**
   * 设置收件人
   *
   * @param to 收件人
   * @return this
   */
  EmailSender to(String to);

  /**
   * 设置抄送人
   *
   * @param cc 抄送人
   * @return this
   */
  EmailSender cc(String cc);

  /**
   * 设置标题
   *
   * @param subject 标题
   * @return this
   */
  EmailSender subject(String subject);

  /**
   * 设置内容
   *
   * @param content 内容
   * @return this
   */
  EmailSender content(String content);

  /** 发送 */
  void send();
}
