package com.rongia.shared.infrastructure.core;

import com.rongia.shared.application.exception.ApiException;
import com.rongia.shared.application.port.EmailSender;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/** 邮件发送器实现（基于 spring-boot-starter-mail） */
@Component
@Scope("prototype")
public class JavaMailEmailSender implements EmailSender {

  /** 邮件发送者 */
  private final JavaMailSender javaMailSender;

  /** 发送人名称 */
  private final String personal;

  /** 收件人 */
  private String to = "";

  /** 抄送人 */
  private String cc = "";

  /** 标题 */
  private String subject = "";

  /** 内容 */
  private String content = "";

  /** 附件列表 */
  private final List<Attachment> attachments = new ArrayList<>();

  /**
   * 构造器注入
   *
   * @param javaMailSender 邮件发送者
   * @param personal 发送人名称
   */
  public JavaMailEmailSender(
      JavaMailSender javaMailSender, @Value("${spring.mail.personal}") String personal) {
    this.javaMailSender = javaMailSender;
    this.personal = personal;
  }

  @Override
  public @NonNull EmailSender attach(@NonNull String fileName, byte[] data) {
    attachments.add(new Attachment(fileName, data));
    return this;
  }

  @Override
  public @NonNull EmailSender to(@NonNull String to) {
    this.to = to;
    return this;
  }

  @Override
  public @NonNull EmailSender cc(@NonNull String cc) {
    this.cc = cc;
    return this;
  }

  @Override
  public @NonNull EmailSender subject(@NonNull String subject) {
    this.subject = subject;
    return this;
  }

  @Override
  public @NonNull EmailSender content(@NonNull String content) {
    this.content = content;
    return this;
  }

  @Override
  public void send() {
    try {
      MimeMessage message = javaMailSender.createMimeMessage();
      MimeMessageHelper helper =
          new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

      // 发送人邮箱直接从 JavaMailSenderImpl 取，等价于 spring.mail.username
      String from = ((JavaMailSenderImpl) javaMailSender).getUsername();
      helper.setFrom(new InternetAddress(from, personal, StandardCharsets.UTF_8.name()));

      helper.setTo(to);
      if (StringUtils.isNotBlank(cc)) {
        helper.setCc(cc);
      }
      helper.setSubject(subject);
      helper.setText(content, true);

      for (Attachment item : attachments) {
        helper.addAttachment(item.fileName(), new ByteArrayResource(item.data()));
      }

      javaMailSender.send(message);
    } catch (Exception e) {
      throw new ApiException("邮件发送失败：" + e.getMessage());
    } finally {
      reset();
    }
  }

  /** 重置状态，避免实例被复用时串号 */
  private void reset() {
    this.to = "";
    this.cc = "";
    this.subject = "";
    this.content = "";
    this.attachments.clear();
  }

  /** 附件 */
  private record Attachment(String fileName, byte[] data) {}
}
