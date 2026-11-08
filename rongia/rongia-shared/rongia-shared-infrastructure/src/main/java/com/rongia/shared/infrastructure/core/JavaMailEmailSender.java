package com.rongia.shared.infrastructure.core;

import com.rongia.shared.application.exception.ApiException;
import com.rongia.shared.application.port.EmailSender;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * 邮件发送器实现（基于 spring-boot-starter-mail）
 *
 * <p>本类是单例，自身不持有邮件内容：链式方法把参数写进调用线程的草稿（{@link Draft}）， {@link #send()} 取草稿发送，发完在 {@code finally}
 * 里清除。因此多个线程共用这一个实例也不会互相覆盖收件人与正文， 同时调用方无需每次新建发送器。
 *
 * <p>草稿按线程累积，所以同一线程内 {@code attach()} 可以先于 {@code to()} 调用（重发邮件即为此顺序）； 但同一线程在 {@code send()}
 * 之前连续组装多封邮件会合并成一封，调用方应组装一封即发送一封。
 */
@NullMarked
@Component
public class JavaMailEmailSender implements EmailSender {

  /** 邮件发送者 */
  private final JavaMailSender javaMailSender;

  /** 发送人名称 */
  private final String personal;

  /** 本线程待发送的邮件草稿 */
  private final ThreadLocal<Draft> pending = ThreadLocal.withInitial(Draft::new);

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

  /**
   * 添加附件，追加到本线程草稿，可多次调用
   *
   * @param fileName 附件名
   * @param data 附件内容
   * @return this
   */
  @Override
  public EmailSender attach(String fileName, byte[] data) {
    pending.get().attachments.add(new Attachment(fileName, data));
    return this;
  }

  /**
   * 设置收件人，多个地址以分号或逗号分隔
   *
   * @param to 收件人
   * @return this
   */
  @Override
  public EmailSender to(String to) {
    pending.get().to = to;
    return this;
  }

  /**
   * 设置抄送人，多个地址以分号或逗号分隔
   *
   * @param cc 抄送人
   * @return this
   */
  @Override
  public EmailSender cc(String cc) {
    pending.get().cc = cc;
    return this;
  }

  /**
   * 设置标题
   *
   * @param subject 标题
   * @return this
   */
  @Override
  public EmailSender subject(String subject) {
    pending.get().subject = subject;
    return this;
  }

  /**
   * 设置正文（HTML）
   *
   * @param content 正文
   * @return this
   */
  @Override
  public EmailSender content(String content) {
    pending.get().content = content;
    return this;
  }

  /**
   * 发送本线程草稿并清除，收件人为空时抛异常
   *
   * @throws ApiException 收件人为空或发送失败时抛出
   */
  @Override
  public void send() {
    Draft draft = pending.get();
    try {
      // MimeMessageHelper 的单串重载只接受一个地址，多收件人必须拆成数组
      String[] toAddresses = splitAddresses(draft.to);
      String[] ccAddresses = splitAddresses(draft.cc);
      if (toAddresses.length == 0) {
        throw new ApiException("邮件收件人不能为空");
      }

      MimeMessage message = javaMailSender.createMimeMessage();
      MimeMessageHelper helper =
          new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

      // 发送人邮箱直接从 JavaMailSenderImpl 取，等价于 spring.mail.username
      String from = ((JavaMailSenderImpl) javaMailSender).getUsername();
      helper.setFrom(new InternetAddress(from, personal, StandardCharsets.UTF_8.name()));

      helper.setTo(toAddresses);
      if (ccAddresses.length > 0) {
        helper.setCc(ccAddresses);
      }
      helper.setSubject(draft.subject);
      helper.setText(draft.content, true);

      for (Attachment item : draft.attachments) {
        helper.addAttachment(item.fileName(), new ByteArrayResource(item.data()));
      }

      javaMailSender.send(message);
    } catch (ApiException e) {
      throw e;
    } catch (Exception e) {
      throw new ApiException("邮件发送失败：" + e.getMessage());
    } finally {
      pending.remove();
    }
  }

  /**
   * 拆分收件人字符串（分号或逗号分隔，忽略空白项）
   *
   * @param value 收件人字符串
   * @return 地址数组
   */
  private static String[] splitAddresses(String value) {
    String[] parts = StringUtils.split(value, ";,");
    if (parts == null) {
      return new String[0];
    }
    return Arrays.stream(parts)
        .map(String::trim)
        .filter(StringUtils::isNotBlank)
        .toArray(String[]::new);
  }

  /** 一封待发送邮件 */
  private static final class Draft {

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
  }

  /** 附件 */
  private record Attachment(String fileName, byte[] data) {}
}
